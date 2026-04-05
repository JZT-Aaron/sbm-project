from fastapi import FastAPI, HTTPException
import docker
from docker.errors import APIError, NotFound
import secrets
import string
import time
import threading
import shutil
from pathlib import Path 
import json
import subprocess
import os
import signal
import math
import uuid

app = FastAPI()
client = docker.from_env()


# Global capacity and port allocation settings
MAX_SERVERS = 4
BASE_PORT = 25566


PASSWORD_FILE = Path("/app/server_credentials.json")

server_credentials = {}
server_credentials_lock = threading.Lock()

def load_credentials():
    global server_credentials
    if PASSWORD_FILE.exists():
        try:
            with open(PASSWORD_FILE, "r") as f:
                server_credentials = json.load(f)
        except Exception as e:
            print(f"Fehler beim Laden der Datei: {e}")
            server_credentials = {}
    else:
        server_credentials = {}

def save_credentials():
    print('Save Creds...')
    PASSWORD_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(PASSWORD_FILE, "w") as f:
        json.dump(server_credentials, f, indent=4)
    print('Save Creds. Finished.')

load_credentials()

def generate_secure_password(length=24):
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for i in range(length))

def get_active_mc_containers():
    return client.containers.list(filters={"label": "managed_by=mc-cloud"})

def get_used_host_ports() -> set[int]:
    used = set()
    # Only running Containers
    for c in client.containers.list():
        ports = c.attrs.get("NetworkSettings", {}).get("Ports", {}) or {}
        for _container_port, host_bindings in ports.items():
            if not host_bindings:
                continue
            for binding in host_bindings:
                host_port = binding.get("HostPort")
                if not host_port:
                    continue
                try:
                    used.add(int(host_port))
                except ValueError:
                    continue
    return used

def ensure_stack_network(network_name: str):
    """Return an existing stack network or create it if missing."""
    try:
        return client.networks.get(network_name)
    except NotFound:
        return client.networks.create(network_name, labels={"managed_by": "mc-cloud"})

def connect_container_to_network_if_needed(network, container):
    """Connect a container to a network and ignore harmless duplicate attach errors."""
    try:
        network.connect(container)
    except docker.errors.APIError as e:
        msg = str(e).lower()
        if "already exists" in msg or "already connected" in msg:
            return
        raise

@app.get("/status")
def get_status():
    active_containers = get_active_mc_containers()
    return {
        "running": math.ceil(len(active_containers)/3),
        "max": MAX_SERVERS,
    }

DATA_ROOT = Path("/data")
TEMPLATE_ROOT = Path("/templates")

def prepare_stack_data(stack_id: str):
    # Build stack-local world/plugin data from templates
    stack_root = DATA_ROOT / stack_id
    mc_dir = stack_root / "mc"

    plugin_src = TEMPLATE_ROOT / "plugins"
    worlds_src = TEMPLATE_ROOT / "worlds"

    mc_dir.mkdir(parents=True, exist_ok=True)

    print(f"Prepare stack data for: {mc_dir} from {worlds_src}")

    if any(mc_dir.iterdir()):
        raise Exception(f"Stack data for {stack_id} already exists")

    if(plugin_src.exists()):
        print(f'Copy Plugins from {plugin_src} to {mc_dir / "plugins"}')
        shutil.copytree(plugin_src, mc_dir / "plugins", dirs_exist_ok=True)

    if(worlds_src.exists()):
        print('Starting to copy worlds')
        for world_entry in worlds_src.iterdir():
            print(f'Copy world {world_entry}')
            target = mc_dir / world_entry.name
            if(world_entry.is_dir()):
                shutil.copytree(world_entry, target, dirs_exist_ok=True)
            else:
                shutil.copy2(world_entry, target)

@app.post("/create-server")
def create_server(player_name: str):
    active_containers = get_active_mc_containers()
    if len(active_containers)/3 >= MAX_SERVERS:
        raise HTTPException(status_code=400, detail="Limit of 4 Servers exsieted")
    
    used_ports= get_used_host_ports()
 
    assigned_port = BASE_PORT
    while assigned_port in used_ports:
        assigned_port += 1

    # Generate secure, non-guessable stack ID
    stack_id = str(uuid.uuid4())[:8]

    pg_pass = generate_secure_password()
    redis_pass = generate_secure_password()
    rcon_pass = generate_secure_password()
    
    network_name = f"net_{stack_id}"
    try:
        ensure_stack_network(network_name)
    except docker.errors.APIError as e:
        raise HTTPException(status_code=500, detail=f"Failed to prepare network {network_name}: {str(e)}")

    # Start in background thread so API responds immediately
    thread = threading.Thread(
        target=start_task,
        args=(stack_id, network_name, assigned_port, player_name, rcon_pass, pg_pass, redis_pass),
        daemon=False
    )
    thread.start()

    IP_PREFIX = "sbm.dachente.de"

    return {"message": "status", "stack_id": stack_id, "ip": f"{IP_PREFIX}:{assigned_port}"}

def start_task(stack_id, network_name, assigned_port, player_name, rcon_pass, pg_pass, redis_pass):
    """Start all containers for a stack with proper error handling and automatic cleanup."""
    with server_credentials_lock:
        server_credentials[stack_id] = {
            "player": player_name,
            "port": assigned_port,
            "redis_pass": redis_pass,
            "pg_pass": pg_pass,
            "rcon_pass": rcon_pass
        }
        save_credentials()
    
    set_server_state(stack_id, "Starting...")
    
    # Shared error/exception tracking
    errors = []
    errors_lock = threading.Lock()
    
    db_name = f"db_{stack_id}"
    redis_name = f"redis_{stack_id}"
    mc_name = f"minecraft_{stack_id}"
    
    def record_error(error_msg: str, exception: Exception = None):
        """Thread-safe error recording."""
        with errors_lock:
            full_msg = f"{error_msg}: {str(exception)}" if exception else error_msg
            errors.append(full_msg)
            print(f"❌ {full_msg}")
    
    def launch_db():
        """Launch PostgreSQL container."""
        try:
            client.containers.run(
                "postgres:16-alpine", name=db_name, detach=True, network=network_name,
                environment={"POSTGRES_USER": "sql_admin", "POSTGRES_PASSWORD": pg_pass, "POSTGRES_DB": "data"},
                labels={"managed_by": "mc-cloud", "stack": stack_id},
                volumes={f"/opt/mc-manager/data/{stack_id}/db": {'bind': '/var/lib/postgresql/data', 'mode': 'rw'}}
            )
            print(f"✓ DB container {db_name} started")
        except Exception as e:
            record_error(f"Failed to launch DB container {db_name}", e)
    
    def launch_redis():
        """Launch Redis container."""
        try:
            client.containers.run(
                "redis:alpine", name=redis_name, detach=True, network=network_name,
                command=f"redis-server --requirepass {redis_pass}",
                labels={"managed_by": "mc-cloud", "stack": stack_id},
                volumes={f"/opt/mc-manager/data/{stack_id}/redis": {'bind': '/data', 'mode': 'rw'}},
            )
            print(f"✓ Redis container {redis_name} started")
        except Exception as e:
            record_error(f"Failed to launch Redis container {redis_name}", e)
    
    def launch_mc():
        """Launch Minecraft container."""
        try:
            prepare_stack_data(stack_id)
            
            client.containers.run(
                "itzg/minecraft-server", name=mc_name, detach=True, network=network_name, 
                restart_policy={"Name": "on-failure", "MaximumRetryCount": 5},
                ports={'25565/tcp': assigned_port},
                environment={
                    "EULA": "TRUE", "TYPE": "PAPER", "VERSION": "1.21.11", "MEMORY": "4G",
                    "LEVEL": "SBM-Lobby",
                    "ONLINE_MODE": "FALSE", "ENABLE_RCON": "true", "RCON_PASSWORD": rcon_pass,
                    "DIFFICULTY": "peaceful",
                    "SQL_HOST": db_name, "SQL_PORT": "5432", "SQL_USER": "sql_admin",
                    "SPIGOT_SETTINGS_CONNECTION_THROTTLE": "0",
                    "PAPER_GLOBAL_MISC_MAX_JOINS_PER_TICK": "20",
                    "PAPER_GLOBAL_PACKET_LIMITER_ALL_PACKETS_MAX_PACKET_RATE": "1000.0",
                    "SQL_PASSWORD": pg_pass, "SQL_DB": "data", "REDIS_HOST": redis_name,
                    "REDIS_PORT": "6379", "REDIS_PASSWORD": redis_pass, "STACK_ID": stack_id, "DEMO": "TRUE"
                },
                labels={"managed_by": "mc-cloud", "stack": stack_id, "player": player_name},
                volumes={f"/opt/mc-manager/data/{stack_id}/mc": {'bind': '/data', 'mode': 'rw'}}
            )
            print(f"✓ MC container {mc_name} started")
        except Exception as e:
            record_error(f"Failed to launch MC container or prepare data", e)
    
    def wait_for_ready():
        """Wait for all containers to be ready."""
        try:
            timeout = time.time() + 300
            ready = False
            
            while not ready and time.time() < timeout:
                try:
                    c_db = client.containers.get(db_name)
                    c_redis = client.containers.get(redis_name)
                    c_mc = client.containers.get(mc_name)
                    
                    # Check DB and Redis are running
                    db_ok = c_db.status == "running"
                    redis_ok = c_redis.status == "running"
                    mc_ready = False
                    
                    if c_mc.status == "running":
                        logs = c_mc.logs(tail=20).decode("utf-8")
                        if "Done" in logs and "For help, type \"help\"" in logs:
                            mc_ready = True
                            time.sleep(2)
                            c_mc.exec_run(f"rcon-cli op {player_name}")
                            print(f"✓ Minecraft server ready and {player_name} is OP")
                    
                    if db_ok and redis_ok and mc_ready:
                        ready = True
                    else:
                        time.sleep(2)
                        
                except NotFound:
                    # Container doesn't exist yet
                    time.sleep(2)
                except Exception as e:
                    print(f"Checking container status: {e}")
                    time.sleep(2)
            
            if not ready:
                record_error(f"Stack failed to start within timeout (300s)")
                return False
            
            set_server_state(stack_id, "Started", True)
            print(f"✓ Stack {stack_id} fully started")
            return True
            
        except Exception as e:
            record_error(f"Error in wait_for_ready", e)
            return False
    
    try:
        # Connect helper containers to stack network (best-effort; do not fail stack startup).
        network = ensure_stack_network(network_name)

        try:
            commander_container = client.containers.get("redis-commander")
            connect_container_to_network_if_needed(network, commander_container)
        except NotFound:
            print("Warning: redis-commander container not found; skipping network attach.")
        except docker.errors.APIError as e:
            if "network" in str(e).lower() and "not found" in str(e).lower():
                network = ensure_stack_network(network_name)
                connect_container_to_network_if_needed(network, commander_container)
            else:
                print(f"Warning: Failed to connect redis-commander to {network_name}: {e}")

        try:
            api_container = client.containers.get("mc-manager-api")
            connect_container_to_network_if_needed(network, api_container)
        except NotFound:
            print("Warning: mc-manager-api container not found; skipping network attach.")
        except docker.errors.APIError as e:
            if "network" in str(e).lower() and "not found" in str(e).lower():
                network = ensure_stack_network(network_name)
                connect_container_to_network_if_needed(network, api_container)
            else:
                print(f"Warning: Failed to connect mc-manager-api to {network_name}: {e}")

        print(f"✓ Helper container attach step finished for {network_name}")
        
        # Launch all core services in parallel
        threads = [
            threading.Thread(target=launch_db, name="launch-db"),
            threading.Thread(target=launch_redis, name="launch-redis"),
            threading.Thread(target=launch_mc, name="launch-mc")
        ]
        
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        
        # Fail fast if one launcher already reported an error
        if errors:
            raise Exception(f"Container launch errors: {'; '.join(errors)}")
        
        # Wait for all services to be ready
        ready = wait_for_ready()
        
        if not ready:
            raise Exception("Stack startup timeout or error")
        
    except Exception as e:
        error_msg = str(e)
        print(f"\n❌ STACK STARTUP FAILED: {error_msg}")
        print(f"Auto-cleaning up {stack_id}...\n")
        set_server_state(stack_id, f"Error: {error_msg}")
        
        # Auto-cleanup on failure to prevent orphaned resources
        try:
            delete_server(stack_id)
            print(f"✓ Stack {stack_id} cleaned up after failure")
        except Exception as cleanup_error:
            print(f"⚠ Cleanup error: {cleanup_error}")
        
        
@app.delete("/delete-server/{stack_id}")
def delete_server(stack_id: str):
    # Ensure spawned Mineflayer bots are terminated before removing infrastructure
    remove_bots(stack_id)

    with server_credentials_lock:
        if stack_id in server_credentials:
            del server_credentials[stack_id]
            save_credentials()               
            print(f"Stack {stack_id} removed from Creds file")
        else:
            print(f"Warning: Stack {stack_id} was not listed.")

    containers = client.containers.list(all=True, filters={"label": f"stack={stack_id}"})
    
    removed_containers = []
    container_errors = []

    for c in containers:
        try:
            c.stop(timeout=10)
        except docker.errors.APIError:
            pass

        try:
            c.remove(force=True, v=True)
            removed_containers.append(c.name)
        except docker.errors.APIError as e:
            container_errors.append(f"{c.name}: {str(e)}")

    network_removed = False
    network_error = None
    try:
        network = client.networks.get(f"net_{stack_id}")
        attached = network.attrs.get("Containers") or {}
        for endpoint_id in list(attached.keys()):
            try:
                attached_container = client.containers.get(endpoint_id)
                network.disconnect(attached_container, force=True)
            except Exception:
                pass

        network.remove()
        network_removed = True
    except docker.errors.NotFound:
        network_removed = False
    except docker.errors.APIError as e:
        network_error = str(e)

    data_path = DATA_ROOT / stack_id
    data_removed = False
    data_error = None
    try:
        if data_path.exists():
            shutil.rmtree(data_path)
            data_removed = True
    except Exception as e:
        data_error = str(e)

    if container_errors or network_error or data_error:
        return {
            "message": f"Stack {stack_id} partially deleted",
            "removed_containers": removed_containers,
            "network_removed": network_removed,
            "data_removed": data_removed,
            "container_errors": container_errors,
            "network_error": network_error,
            "data_error": data_error,
        }

    return {
        "message": f"Stack {stack_id} successfully deleted",
        "removed_containers": removed_containers,
        "network_removed": network_removed,
        "data_removed": data_removed,
    }

server_states = {}

def set_server_state(stack_id: str, state: str, started = False):
    server_states[stack_id] = {"state": state, "started": started}

def get_server_state(stack_id: str):
    return server_states.get(stack_id, {"state": "Unknown", "started": False})

@app.get("/state/{stack_id}")
def get_state(stack_id: str):
    return get_server_state(stack_id)

@app.delete("/clear-all")
def clear_all():
    containers = client.containers.list(all=True, filters={"label": "managed_by=mc-cloud"})
    
    deleted_stacks = []
    errors = []
    
    stack_ids = set()
    for c in containers:
        container_name = c.name
        if "_" in container_name:
            stack_id = container_name.split("_", 1)[1]
            stack_ids.add(stack_id)
    
    for stack_id in stack_ids:
        try:
            result = delete_server(stack_id)
            deleted_stacks.append({
                "stack_id": stack_id,
                "result": result
            })
        except Exception as e:
            errors.append(f"{stack_id}: {str(e)}")
    
    return {
        "message": "Clear all completed",
        "deleted_stacks": deleted_stacks,
        "errors": errors,
    }

import redis

@app.get("/get-redis-data/{stack_id}")
def get_redis_data(stack_id: str):
    print(f"Start to look for redis data: {stack_id}")

    creds = server_credentials.get(stack_id)
    print(f"Stack id creds found: {creds}")
    if not creds:
        return {"deleted":True}

    try:
        r = redis.Redis(host=f"redis_{stack_id}", port=6379, password=creds["redis_pass"], decode_responses=True, socket_connect_timeout=2)
        game_stats = r.hgetall("game:stats") or {}

        # Parse current game lifecycle state
        raw_state = game_stats.get("state", "IDLE").strip('"')
        print(f"DEBUG: {raw_state}")
        game_started = raw_state in ["RUNNING_MATCH", "RUNNING_REMATCH"]
        game_starting = "STARTING" in raw_state
        game_paused = raw_state == "PAUSED"

        # Parse team-heart payload safely from JSON text
        def get_hearts(data_str):
            if not data_str or data_str.lower() == "null":
                return {"RED": 0, "BLUE": 0}
            try:
                return json.loads(data_str)
            except:
                return {"RED": 0, "BLUE": 0}

        hearts_data = get_hearts(game_stats.get("team-hearts"))
        red_hearts = int(hearts_data.get("RED", 0))
        blue_hearts = int(hearts_data.get("BLUE", 0))

        game_end_millis_raw = game_stats.get("game-end-timestamp", 0)
        try:
            game_end_millis = int(game_end_millis_raw)
        except:
            game_end_millis = 0

        # Normalized response consumed by the frontend
        return {
            "game_started": game_started,
            "game_starting": game_starting,
            "paused": game_paused,
            "game_state": raw_state,
            "red-hearts": red_hearts,
            "blue-hearts": blue_hearts,
            "game_end_millis": game_end_millis
        }

    except Exception as e:
        # Fallback response on Redis or parsing errors
        return {
            "game_started": False,
            "paused": False,
            "game_starting": False,
            "game_state": "ERROR",
            "red-hearts": 0,
            "blue-hearts": 0,
            "game_end_millis": 0,
            "error_details": str(e)
        }

def run_game_command(stack_id: str, command: str):
    try:
        c_mc = client.containers.get(f"minecraft_{stack_id}")
        c_mc.exec_run(f"rcon-cli game {command}")
    except NotFound:
        raise HTTPException(status_code=404, detail=f"Minecraft container for {stack_id} not found")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error executing game command: {str(e)}")

@app.post('/game')
def gamestart(stack_id: str, command: str):
    run_game_command(stack_id, command)
    return {"status": "Done!"}

def spawn_minecraft_bot(stack_id, bot_name):
    # Start one external Node.js Mineflayer process per bot
    script_path = "/app/bot_template.js"
    
    bot_env = os.environ.copy()
    bot_env.update({
        "MC_HOST": f"minecraft_{stack_id}",
        "BOT_NAME": bot_name,
        "NODE_PATH": "/app/node_modules"
    })

    # Forward bot logs to the API container logs for easier debugging
    process = subprocess.Popen(
        ["node", script_path], 
        env=bot_env,
        stdout=None,
        stderr=None
    )
    return process.pid

@app.post("/add-bots/{stack_id}")
def add_bots(stack_id: str):
    # Open joining and competition state before spawning bots
    if stack_id not in server_credentials:
        raise HTTPException(status_code=404, detail="Stack nicht gefunden")

    mc_c = client.containers.get(f"minecraft_" + stack_id)
    mc_c.exec_run("rcon-cli game game-joining on")
    mc_c.exec_run("rcon-cli game open")

    # Ensure game joining is enabled before bot login

    
    # Create credentials section for bots on first run
    if "bots" not in server_credentials[stack_id]:
        server_credentials[stack_id]["bots"] = []

    new_bot_data = []
    
    for i in range(1, 4):
        # Unique name with prefix and short timestamp suffix
        timestamp = int(time.time() * 1000) % 10000
        bot_name = f"Bot_{stack_id[:3]}_{i}_{timestamp}"
        
        # Spawn bot process
        pid = spawn_minecraft_bot(stack_id, bot_name)
        
        # Store metadata to support later cleanup/removal
        bot_info = {
            "name": bot_name,
            "pid": pid,
            "started_at": time.strftime("%Y-%m-%d %H:%M:%S")
        }
        
        server_credentials[stack_id]["bots"].append(bot_info)
        new_bot_data.append(bot_info)
        
        # Stagger logins to reduce join pressure on the MC server
        time.sleep(5)

    # Persist bot process metadata
    save_credentials()

    return {
        "status": "3 Bots erfolgreich registriert und gespeichert",
        "stack": stack_id,
        "active_bots": new_bot_data
    }

@app.delete("/remove-bots/{stack_id}")
def remove_bots_endpoint(stack_id: str):
    success = remove_bots(stack_id)
    if not success:
        raise HTTPException(status_code=404, detail="Keine laufenden Bots gefunden.")
    return {"message": f"Alle Bots für {stack_id} wurden entfernt."}

def remove_bots(stack_id: str):
    # Stop all bot processes and clear their team assignment in-game
    creds = server_credentials.get(stack_id)
    if not creds or "bots" not in creds or not creds["bots"]:
        return False
    
    try:
        c_mc = client.containers.get(f"minecraft_{stack_id}")
    except NotFound:
        print(f"Warning: Minecraft container for {stack_id} not found, skipping rcon commands")
        c_mc = None

    for bot in creds["bots"]:
        pid = bot.get("pid")
        bot_name = bot.get("name")
        try:
            if c_mc:
                c_mc.exec_run(f"rcon-cli team remove {bot_name}")
            os.kill(pid, signal.SIGTERM)
            print(f"Bot {bot_name} (PID {pid}) beendet.")
        except ProcessLookupError:
            print(f"Bot {bot_name} lief nicht mehr.")
        except Exception as e:
            print(f"Fehler beim Beenden von {bot_name}: {e}")

    # Clear bot list from stored credentials
    with server_credentials_lock:
        creds["bots"] = []
        save_credentials()
    return True