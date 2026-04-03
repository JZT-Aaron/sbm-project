package de.dachente.sbm.main;

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
import org.bukkit.entity.ArmorStand;
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
import de.dachente.sbm.listeners.DamageByEntityListener;
import de.dachente.sbm.listeners.DamageListener;
import de.dachente.sbm.listeners.InteractListener;
import de.dachente.sbm.listeners.InventoryClickListener;
import de.dachente.sbm.listeners.InventoryOpenListener;
import de.dachente.sbm.listeners.ItemDropListener;
import de.dachente.sbm.listeners.JoinListener;
import de.dachente.sbm.listeners.MoveListener;
import de.dachente.sbm.listeners.MutliLangSignManager;
import de.dachente.sbm.listeners.PlayerToggleSnakeListener;
import de.dachente.sbm.listeners.QuitListener;
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
import de.dachente.sbm.utils.Repeat;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.coms.BackendClient;
import de.dachente.sbm.utils.coms.DatabaseManager;
import de.dachente.sbm.utils.coms.RedisEventPublisher;
import de.dachente.sbm.utils.coms.RedisManager;
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
    public static NamespacedKey TAG_KEY;

    private static BackendClient backendClient;
    private static RedisEventPublisher redisEventPublisher;
    private static RedisManager redisManager;
    private static DatabaseManager dbManager;

    public static List<Player> resendLobbySigns = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("Plugin is starting ...");

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

        for(ArmorStand armorStand : Game.cameraPoints) {
            armorStand.remove();
        }

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

        pluginManager.registerEvents(new PlayerToggleSnakeListener(), this);

        pluginManager.registerEvents(new DamageByEntityListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new SnowballHitListener(), this);

        pluginManager.registerEvents(new JoinListener(), this);
        pluginManager.registerEvents(new AsyncPlayerPreLoginListener(), this);
        pluginManager.registerEvents(new QuitListener(), this);
    }

    private void registerTeamGates() {
        List<Location> bluePressurePlates = new ArrayList<>();
        List<Location> redPressurePlates = new ArrayList<>();
        Map<Location, List<Location>> barriersPerGate = new HashMap<>();

        List<Location> blueSigns = new ArrayList<>();
        List<Location> redSigns = new ArrayList<>();
        
        ConfigurationSection plateSection = getConfig().getConfigurationSection("gates-pos.plates");
        ConfigurationSection barrierSection = getConfig().getConfigurationSection("gates-pos.barriers");
        ConfigurationSection signSection = getConfig().getConfigurationSection("gates-pos.signs");
        
        for(String key : plateSection.getKeys(false)) {
            List<Location> plateLocations = parseList(plateSection.getString(key));
            List<Location> barrierLocations = parseList(barrierSection.getString(key));

            blueSigns.addAll(parseList(signSection.getString(key)));
            redSigns.addAll(blueSigns.stream().map(this::getMirrorLocation).toList());

            List<Location> mirrorBarrierLocations = barrierLocations.stream().map(this::getMirrorLocation).toList();

            for(Location plateLocation : plateLocations) {
                Location redPlate = getMirrorLocation(plateLocation);
                barriersPerGate.put(redPlate, mirrorBarrierLocations);
                barriersPerGate.put(plateLocation, barrierLocations);
                bluePressurePlates.add(plateLocation);
                redPressurePlates.add(redPlate);
            }

        }

        GateManager.setPressurePlates(bluePressurePlates, redPressurePlates);
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
        String[] chunks = Main.getPlugin().getConfig().getString("spawn-points.forceload-chunks").split(";");
        for(String chunk : chunks) {
            String[] cords = chunk.split(",");
            chunksList.add(Main.arena.getChunkAt(Integer.parseInt(cords[0]), Integer.parseInt(cords[1])));
        }
        return chunksList;
    }

    private void registerCameras() {
        ArmorStand cameraUp = (ArmorStand) arena.spawnEntity(new Location(arena, 0, 22, 0, 180, 90), EntityType.ARMOR_STAND);
        cameraUp.customName(Component.text("camera-up"));
        ArmorStand cameraRedSide = (ArmorStand) arena.spawnEntity(new Location(arena, -10, 9, -15,-50,50), EntityType.ARMOR_STAND);
        cameraRedSide.customName(Component.text("camera-red-side"));
        ArmorStand cameraBlueSide = (ArmorStand) arena.spawnEntity(new Location(arena, 10, 9, 15, 150, 50), EntityType.ARMOR_STAND);
        cameraBlueSide.customName(Component.text("camera-blue-side"));

        Game.cameraPoints.add(cameraUp);
        Game.cameraPoints.add(cameraBlueSide);
        Game.cameraPoints.add(cameraRedSide);

        for(ArmorStand camera : Game.cameraPoints) {
            camera.setMarker(true);
            camera.setInvulnerable(true);
            camera.setVisible(false);
            camera.setBasePlate(false);
            camera.setGravity(false);
        }
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
        Game.setServerHotbar(server, player);
        StatusManger.setPlayerStatus(server.getStatus(), player);
        if(!keepPosition) player.teleport(server.getWorld().getSpawnLocation());
        if(server == Server.EVENT_SERVER) {
            MutliLangSignManager.customSigns.remove(player.getUniqueId());
            if(Game.isRunning()) {
                if(!BossBarManager.containsPlayer(player)) BossBarManager.addPlayer(player);  
            } 
        } else {
            if(Server.LOBBY == server) { Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> 
                                        StartClock.updateSigns(player, true), 10L);
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
