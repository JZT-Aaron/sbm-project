package de.dachente.sbm.main;

import de.dachente.sbm.commands.*;
import de.dachente.sbm.listeners.*;
import de.dachente.sbm.tabs.GameTab;
import de.dachente.sbm.tabs.InfoTab;
import de.dachente.sbm.tabs.TeamTab;
import de.dachente.sbm.tabs.VoidTab;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Repeat;
import de.dachente.sbm.utils.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class Main extends JavaPlugin {

    public static World lobby;
    public static World arena;
    private static Main plugin;
    public static NamespacedKey NO_MOVE;
    public static NamespacedKey TAG_KEY;

    @Override
    public void onEnable() {
        getLogger().info("Plugin is starting ...");

        plugin = this;
        saveDefaultConfig();

        new WorldCreator("SBM-Arena").createWorld();

        lobby = Bukkit.getWorld("SBM-Lobby");
        arena = Bukkit.getWorld("SBM-Arena");

        NO_MOVE = new NamespacedKey(this, "no-move");
        TAG_KEY = new NamespacedKey(this, "tag-data");

        Repeat.start();

        registerCommands();
        registerEvents();
        registerCameras();
        registerTeamGates();

        getLogger().info("Plugin is started.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin is stopping ...");

        Repeat.stop();

        for(ArmorStand armorStand : Game.cameraPoints) {
            armorStand.remove();
        }

        for(Map.Entry<String, Team> map : Game.getTeamsPlayer().entrySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(map.getKey()));
            if(player == null) return;
            Game.removePlayerTeam(map.getKey());
        }

        Game.bossBar.setVisible(false);
        Game.bossBar.removeAll();
        Game.livingPlayersTeamBlue.clear();
        Game.livingPlayersTeamRed.clear();

        getLogger().info("Plugin is stopped.");
    }

    private void registerCommands() {
        getCommand("alert").setExecutor(new InfoCommand());
        getCommand("alert").setTabCompleter(new InfoTab());
        getCommand("alert").setPermission(getConfig().getString("permission.sbm.command.info"));

        getCommand("game").setExecutor(new GameCommand());
        getCommand("game").setTabCompleter(new GameTab());
        getCommand("game").setPermission(getConfig().getString("permission.sbm.command.game"));

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

        pluginManager.registerEvents(new PlayerToggleSnakeListener(), this);

        pluginManager.registerEvents(new DamageByEntityListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new SnowballHitListener(), this);

        pluginManager.registerEvents(new JoinListener(), this);
        pluginManager.registerEvents(new QuitListener(), this);
    }

    private void registerTeamGates() {
        List<Location> bluePressurePlates = new ArrayList<>();
        List<Location> redPressurePlates = new ArrayList<>();
        Map<Location, List<Location>> barriersPerGate = new HashMap<>();
        
        ConfigurationSection plateSection = getConfig().getConfigurationSection("gates-pos.plates");
        ConfigurationSection barrierSection = getConfig().getConfigurationSection("gates-pos.barriers");
        
        for(String key : plateSection.getKeys(false)) {
            List<Location> plateLocations = parseList(plateSection.getString(key));
            List<Location> barrierLocations = parseList(barrierSection.getString(key));

            List<Location> mirrorBarrierLocations = barrierLocations.stream().map(this::getMirrorLocation).toList();

            for(Location plateLocation : plateLocations) {
                Location redPlate = getMirrorLocation(plateLocation);
                barriersPerGate.put(redPlate, mirrorBarrierLocations);
                barriersPerGate.put(plateLocation, barrierLocations);
                bluePressurePlates.add(plateLocation);
                redPressurePlates.add(redPlate);
            }

        }

        GateManager.setBluePressurePlates(bluePressurePlates, redPressurePlates);
        GateManager.setBarrierPerGate(barriersPerGate);
    }

    private Location getMirrorLocation(Location loc) {
        Location mirror = loc.clone();
        mirror.setZ(mirror.getZ()*-1);
        return mirror;
    }

    private List<Location> parseList(String string) {
        List<Location> locations = new ArrayList<>();
        String[] locs = string.split(";");
        for(String loc : locs) {
            Double[] cords = Arrays.stream(loc.split(","))
                .map(Double::valueOf)
                .toArray(Double[]::new);
            Location l = new Location(arena, cords[0], cords[1], cords[2]);
            locations.add(l);
        }
        return locations;
    }

    private void loadLang() {
        for(Language lang : Language.values()) {
            lang.setFile(getLangFile(lang.getFileName()));
        }
    }

    private FileConfiguration getLangFile(String lang) {    
        String path = "lang/lang_" + lang + ".yml";
        File file = new File(getDataFolder(), path);
        
        if(!file.exists()) {
            saveResource(path, false);
        }

        return YamlConfiguration.loadConfiguration(file);
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

    public static Consumer<String> getCmdReplyConsumer(String senderName, Player player) {
        return msg -> Game.sendInfo(msg, senderName, player);
    }

    public static String toPlain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
