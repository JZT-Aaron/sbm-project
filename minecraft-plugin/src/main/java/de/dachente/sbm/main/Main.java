package de.dachente.sbm.main;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import de.dachente.sbm.commands.GameCommand;
import de.dachente.sbm.commands.GameServerCommand;
import de.dachente.sbm.commands.InfoCommand;
import de.dachente.sbm.commands.LobbyCommand;
import de.dachente.sbm.commands.MapsCommand;
import de.dachente.sbm.commands.TeamCommand;
import de.dachente.sbm.listeners.AsyncPlayerPreLoginListener;
import de.dachente.sbm.listeners.BellRingListener;
import de.dachente.sbm.listeners.BlockBreakListener;
import de.dachente.sbm.listeners.BlockRedstoneHandler;
import de.dachente.sbm.listeners.CommandListener;
import de.dachente.sbm.listeners.DamageByEntityListener;
import de.dachente.sbm.listeners.DamageListener;
import de.dachente.sbm.listeners.InteractListener;
import de.dachente.sbm.listeners.InventoryClickListener;
import de.dachente.sbm.listeners.InventoryOpenListener;
import de.dachente.sbm.listeners.ItemDropListener;
import de.dachente.sbm.listeners.JoinListener;
import de.dachente.sbm.listeners.MoveListener;
import de.dachente.sbm.listeners.MutliLangSignManager;
import de.dachente.sbm.listeners.SpectatorManager;
import de.dachente.sbm.listeners.QuitListener;
import de.dachente.sbm.listeners.SnowballFlyListener;
import de.dachente.sbm.listeners.SnowballHitListener;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.tabs.GameTab;
import de.dachente.sbm.tabs.InfoTab;
import de.dachente.sbm.tabs.MapsTab;
import de.dachente.sbm.tabs.TeamTab;
import de.dachente.sbm.tabs.VoidTab;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.GameRepeat;
import de.dachente.sbm.utils.GameStats;
import de.dachente.sbm.utils.PlayerStats;
import de.dachente.sbm.utils.Repeat;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.coms.BackendClient;
import de.dachente.sbm.utils.coms.DatabaseManager;
import de.dachente.sbm.utils.coms.RedisEventPublisher;
import de.dachente.sbm.utils.coms.RedisManager;
import de.dachente.sbm.utils.enums.Gate;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.Server;
import io.github.cdimascio.dotenv.Dotenv;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import redis.clients.jedis.Jedis;

public final class Main extends JavaPlugin {

    public static World lobby;
    public static World arena;
    private static Main plugin;
    public static NamespacedKey NO_MOVE;
    public static NamespacedKey NO_DROP;
    public static NamespacedKey NO_USE;
    public static NamespacedKey TAG_KEY;

    public static boolean isDemo = false;

    private static BackendClient backendClient;
    private static RedisEventPublisher redisEventPublisher;
    private static RedisManager redisManager;
    private static DatabaseManager dbManager;

    public static List<Player> resendLobbySigns = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("Plugin is starting ...");
        
        isDemo = System.getenv("DEMO") != null && System.getenv("DEMO").equals("TRUE");
        if(isDemo) getLogger().warning("This Plugin was started in DEMO MODE");

        plugin = this;
        saveDefaultConfig();
        for(Language lang : Language.values()) {
            saveResource("lang/lang_" + lang.getFileName() + ".yml", true);
        }

        LanguageManager.loadLang();
        MutliLangSignManager.registerListener();

        new WorldCreator(Server.EVENT_SERVER.getWorldName()).createWorld();

        lobby = Bukkit.getWorld(Server.LOBBY.getWorldName());
        arena = Bukkit.getWorld(Server.EVENT_SERVER.getWorldName());

        Server.LOBBY.setWorld(lobby);
        Server.EVENT_SERVER.setWorld(arena);

        NO_MOVE = new NamespacedKey(this, "no-move");
        NO_DROP = new NamespacedKey(this, "no-drop");
        NO_USE = new NamespacedKey(this, "no-use");
        TAG_KEY = new NamespacedKey(this, "tag-data");

        registerCommands();
        registerEvents();
        registerCameras();
        registerTeamGates();
        loadBackendClient();
        registerEventInfoSigns();

        GameStats.init();
        PlayerStats.initSnowList();

        Repeat.start();
        GameRepeat.start();

        for(Player all : Bukkit.getOnlinePlayers()) LanguageManager.addOnlineSnyc(all.getUniqueId());

        if(Game.isRunning()) {
            BossBarManager.setVisible(true);
            for(Player eventServerPlayer : arena.getPlayers()) BossBarManager.addPlayer(eventServerPlayer);
            Game.updateTeamHearts();
        }
       
        getLogger().info("Plugin is started.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin is stopping ...");

        Repeat.stop();
        GameRepeat.stop();

        if(dbManager != null) dbManager.close();

        BossBarManager.removeAll();

        getLogger().info("Plugin is stopped.");
    }

    private void loadBackendClient() {
        String backendUrl = get("BACKEND_URL");
        String apiKey = get("BACKEND_API_KEY");

        backendClient = new BackendClient(backendUrl, apiKey);
        getLogger().info("BackendClient initialized: " + backendUrl);

        String redisHost = get("REDIS_HOST");
        int redisPort = Integer.parseInt(get("REDIS_PORT"));
        String redisPasswort = get("REDIS_PASSWORD");

        Jedis jedis = new Jedis(redisHost, redisPort);
        jedis.auth(redisPasswort);

        redisEventPublisher = new RedisEventPublisher(jedis);
        getLogger().info("RedisEventPublisher initialized: " + backendUrl);

        redisManager = new RedisManager(redisHost, redisPort, redisPasswort);

        String dbHost = get("SQL_HOST");
        int dbPort = Integer.parseInt(get("SQL_PORT"));
        String user = get("SQL_USER");
        String dbPasswort = get("SQL_PASSWORD");
        String dbDB = get("SQL_DB");

        dbManager = new DatabaseManager(dbHost, dbPort, dbDB, user, dbPasswort);

        PlayerStats.setupDatebase();
    }


    private String get(String key) {
        Dotenv dotenv = Dotenv.configure().directory("/data/").ignoreIfMissing().load();
        return dotenv.get(key) == null ? System.getenv(key) : dotenv.get(key);
    }

    public static BackendClient getBackendClient() {
        return backendClient;
    }

    public static RedisEventPublisher getRedisEventPublisher() {
        return redisEventPublisher;
    }

    public static RedisManager getRedisManager() {
        return redisManager;
    }

    public static DatabaseManager getDbManager() {
        return dbManager;
    }

    private void registerCommands() {
        getCommand("alert").setExecutor(new InfoCommand());
        getCommand("alert").setTabCompleter(new InfoTab());
        getCommand("alert").setPermission(getConfig().getString("permission.sbm.command.info"));

        getCommand("game").setExecutor(new GameCommand());
        getCommand("game").setTabCompleter(new GameTab());
        getCommand("game").setPermission(getConfig().getString("permission.sbm.command.game"));

        getCommand("maps").setExecutor(new MapsCommand());
        getCommand("maps").setTabCompleter(new MapsTab());
        getCommand("maps").setPermission(getConfig().getString("permission.sbm.command.maps"));

        getCommand("game-server").setExecutor(new GameServerCommand());
        getCommand("game-server").setTabCompleter(new VoidTab());

        getCommand("team").setExecutor(new TeamCommand());
        getCommand("team").setTabCompleter(new TeamTab());
        getCommand("team").setPermission(getConfig().getString("permission.sbm.command.team"));

        getCommand("lobby").setExecutor(new LobbyCommand());
        getCommand("lobby").setTabCompleter(new VoidTab());
    }

    private void registerEvents() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(new BlockBreakListener(), this);
        pluginManager.registerEvents(new InteractListener(), this);
        pluginManager.registerEvents(new InventoryClickListener(), this);
        pluginManager.registerEvents(new MoveListener(), this);
        pluginManager.registerEvents(new ItemDropListener(), this);
        pluginManager.registerEvents(new BlockRedstoneHandler(), this);
        pluginManager.registerEvents(new InventoryOpenListener(), this);
        pluginManager.registerEvents(new BellRingListener(), this);

        pluginManager.registerEvents(new SpectatorManager(), this);

        pluginManager.registerEvents(new DamageByEntityListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new SnowballHitListener(), this);
        pluginManager.registerEvents(new SnowballFlyListener(), this);

        pluginManager.registerEvents(new JoinListener(), this);
        pluginManager.registerEvents(new AsyncPlayerPreLoginListener(), this);
        pluginManager.registerEvents(new QuitListener(), this);

        if(isDemo) pluginManager.registerEvents(new CommandListener(), this);
    }

    private void registerTeamGates() {
        Map<Location, Gate> pressurePlates = new HashMap<>();
        Map<Gate, List<Location>> barriersPerGate = new HashMap<>();
        Map<Gate, List<Location>> barrierBox = new HashMap<>();

        List<Location> blueSigns = new ArrayList<>();
        List<Location> redSigns = new ArrayList<>();
        
        ConfigurationSection plateSection = getConfig().getConfigurationSection("gates-pos.plates");
        ConfigurationSection barrierSection = getConfig().getConfigurationSection("gates-pos.barriers");
        ConfigurationSection barrierBoxSection = getConfig().getConfigurationSection("gates-pos.barriers-box");
        ConfigurationSection signSection = getConfig().getConfigurationSection("gates-pos.signs");
        
        for(String key : plateSection.getKeys(false)) {
            Gate gate = Gate.getGateByString(key);
            List<Location> plateLocations = parseList(plateSection.getString(key));
            List<Location> barrierLocations = parseList(barrierSection.getString(key));
            List<Location> barrierBoxLocations = parseList(barrierBoxSection.getString(key));

            blueSigns.addAll(parseList(signSection.getString(key)));
            redSigns.addAll(blueSigns.stream().map(this::getMirrorLocation).toList());

            List<Location> mirrorBarrierLocations = barrierLocations.stream().map(this::getMirrorLocation).toList();
            List<Location> mirrorBarrierBoxLocations = barrierBoxLocations.stream().map(this::getMirrorLocation).toList();

            for(Location plateLocation : plateLocations) {
                pressurePlates.put(plateLocation, gate);
                pressurePlates.put(getMirrorLocation(plateLocation), Gate.getOppositColor(gate));
            }
            barriersPerGate.put(gate, barrierLocations);
            barriersPerGate.put(Gate.getOppositColor(gate), mirrorBarrierLocations);
            barrierBox.put(gate, barrierBoxLocations);
            barrierBox.put(Gate.getOppositColor(gate), mirrorBarrierBoxLocations);
        }
        

        GateManager.setPressurePlates(pressurePlates);
        GateManager.setBarrierBox(barrierBox);
        GateManager.setBarrierPerGate(barriersPerGate);
        GateManager.setSigns(blueSigns, redSigns);
    }

    private void registerEventInfoSigns() {
        StartClock.signs = parseList(getConfig().getString("server-open-signs"), lobby);
    }

    private Location getMirrorLocation(Location loc) {
        Location mirror = loc.clone();
        mirror.setZ(mirror.getZ()*-1);
        return mirror;
    }

    public static List<Location> parseList(String string) {
        return parseList(string, arena);
    }

    public static List<Location> parseList(String string, World world) {
        List<Location> locations = new ArrayList<>();
        String[] locs = string.split(";");
        
        for(String loc : locs) locations.add(parseLocation(loc, world));
        
        return locations;
    }

    public static Location parseLocation(String loc, World world) {
        Double[] cords = Arrays.stream(loc.split(","))
                .map(Double::valueOf)
                .toArray(Double[]::new);
        Location location = new Location(world, cords[0], cords[1], cords[2]);
        if(cords.length == 5) {
            location.setYaw(cords[3].floatValue());
            location.setPitch(cords[4].floatValue());
        }
        return location;
    }

    public static List<Chunk> getForceLoadChunks() {
        List<Chunk> chunksList = new ArrayList<>();
        String[] chunks = Main.getPlugin().getConfig().getString("forceload-chunks").split(";");
        for(String chunk : chunks) {
            String[] cords = chunk.split(",");
            chunksList.add(Main.arena.getChunkAt(Integer.parseInt(cords[0]), Integer.parseInt(cords[1])));
        }
        return chunksList;
    }

    private void registerCameras() {
        ConfigurationSection cameraPos = Main.getPlugin().getConfig().getConfigurationSection("camera-pos");
        Game.cameraPoints.put("camera-up", parseLocation(cameraPos.getString("top"), arena));
        Game.cameraPoints.put("camera-red-side", parseLocation(cameraPos.getString("red"), arena));
        Game.cameraPoints.put("camera-blue-side", parseLocation(cameraPos.getString("blue"), arena));
    }

    public static Main getPlugin() {
        return plugin;
    }

    public static String toPlain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static void joinServer(Server server, Player player) {
        joinServer(server, player, false);
    }

    public static void joinServer(Server server, Player player, boolean keepPosition) {
        StatusManger.setPlayerStatus(server.getStatus(), player);
        if(SpectatorManager.getSpectatorPlayers().containsKey(player.getUniqueId().toString())) {
            SpectatorManager.removeSpectatorPlayers(player);
        }
        if(server == Server.EVENT_SERVER) {
            Game.setViewer(player);
            MutliLangSignManager.customSigns.remove(player.getUniqueId());
            if(Game.isRunning()) {
                if(!BossBarManager.containsPlayer(player)) BossBarManager.addPlayer(player);  
            } 
        } else {
            if(!keepPosition) player.teleport(server.getWorld().getSpawnLocation());
            Game.setServerHotbar(server, player);
            if(Server.LOBBY == server) { 
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> StartClock.updateSigns(player, true), 10L);
                resendLobbySigns.add(player);
            }
            BossBarManager.removePlayer(player);
        }
    }

    public static void clearDroppedItems() {
        for(Entity entity : Main.arena.getEntities()) {
            if(!entity.getType().equals(EntityType.ITEM) ||
                    (entity.getLocation().distanceSquared(parseLocation(getPlugin().getConfig().getString("arena-center"), arena)) > 1600)) continue;
            entity.remove();
        }
    }
}
