package de.dachente.sbm.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import de.dachente.sbm.commands.GameCommand;
import de.dachente.sbm.commands.GameServerCommand;
import de.dachente.sbm.commands.InfoCommand;
import de.dachente.sbm.commands.LobbyCommand;
import de.dachente.sbm.commands.TeamCommand;
import de.dachente.sbm.listeners.BlockBreakListener;
import de.dachente.sbm.listeners.BlockRedstoneHandler;
import de.dachente.sbm.listeners.DamageByEntityListener;
import de.dachente.sbm.listeners.DamageListener;
import de.dachente.sbm.listeners.InteractListener;
import de.dachente.sbm.listeners.InventoryClickListener;
import de.dachente.sbm.listeners.ItemDropListener;
import de.dachente.sbm.listeners.JoinListener;
import de.dachente.sbm.listeners.MoveListener;
import de.dachente.sbm.listeners.PlayerToggleSnakeListener;
import de.dachente.sbm.listeners.QuitListener;
import de.dachente.sbm.listeners.SnowballHitListener;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.tabs.GameTab;
import de.dachente.sbm.tabs.InfoTab;
import de.dachente.sbm.tabs.TeamTab;
import de.dachente.sbm.tabs.VoidTab;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Repeat;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.SignFrame;
import de.dachente.sbm.utils.enums.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Main extends JavaPlugin {

    public static World lobby;
    public static World arena;
    private static Main plugin;
    public static NamespacedKey NO_MOVE;
    public static NamespacedKey TAG_KEY;

    private static Map<String, Status> playerStatus = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Plugin is starting ...");

        plugin = this;
        saveDefaultConfig();
        loadLang();

        new WorldCreator(Server.EVENT_SERVER.getWorldName()).createWorld();

        lobby = Bukkit.getWorld(Server.LOBBY.getWorldName());
        arena = Bukkit.getWorld(Server.EVENT_SERVER.getWorldName());

        Server.LOBBY.setWorld(lobby);
        Server.EVENT_SERVER.setWorld(arena);

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

        Game.hardReset();

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

    public static void joinServer(Server server, Player player) {
        joinServer(server, player, false);
    }

    // Player Status

    public static Status getPlayerStatus(Player player) {
        return playerStatus.get(player.getUniqueId().toString());
    }

    public static void setPlayerStatus(Status status, Player player) {
        playerStatus.remove(player.getUniqueId().toString());
        playerStatus.put(player.getUniqueId().toString(), status);
        updatePlayerStatus(status, player);
    }

    public static void updatePlayerStatus(Player player) {
        updatePlayerStatus(getPlayerStatus(player), player);
    }    

    private static void updatePlayerStatus(Status status, Player player) {
        String playerColor = Game.isInTeam(player) ? Game.getTeam(player).getChatColor() : "§f";
        player.playerListName(Component.text(" " + status.getSymbol() + " §7| " + playerColor + toPlain(player.displayName())));
    }

    public static void joinServer(Server server, Player player, boolean keepPosition) {
        Game.setServerHotbar(server, player);
        setPlayerStatus(server.getStatus(), player);
        if(!keepPosition) player.teleport(server.getWorld().getSpawnLocation());
        if(server == Server.EVENT_SERVER) {
            if(Game.isRoundGoing) {
                if(!Game.bossBar.getPlayers().contains(player)) Game.bossBar.addPlayer(player);
            } 
        }
    }
}
