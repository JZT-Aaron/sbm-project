package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.MapManager;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.GameMap;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Dropper;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Game {

    static FileConfiguration config = Main.getPlugin().getConfig();
    static int taskID;
    static int taskID2;

    public static boolean isSnowing = true;
    
    public static List<ArmorStand> cameraPoints = new ArrayList<>();

    private static GameState beforePause;

    public static ItemStack snowball = new ItemStack(Material.SNOWBALL);

    private static final int ROUND_LENGHT = 40;

    public static void setSnowing(boolean snowing) {
        isSnowing = snowing;
    }

    public static void loadLobbyInv(Player player) {
        StatusManger.setPlayerStatus(Status.WAITING, player);

        UUID uuid = player.getUniqueId();

        ItemStack leaveTeam = new ItemBuilder(Material.RED_BED).setLangNameDescriptionTag("leave-team", uuid).setUnmovable().build();
        player.getInventory().setItem(8, leaveTeam);
        player.getInventory().setItem(7, LanguageManager.getLanguageChangeItem(uuid));
    }

    public static void setViewer(Player player) {
        player.teleport(Main.arena.getSpawnLocation());
        player.getInventory().clear();
        if(TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) TeamManager.setTeamChestPlate(player);
        else StatusManger.setPlayerStatus(Status.WATCHING, player);
        player.setHealthScale(20);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 255,false, false, false));
        setGameServerHotbar(player);
    }

    public static void open() {
        config.set("game.open", true);
        Main.getPlugin().saveConfig();

        for(Player player : Main.lobby.getPlayers()) {
            setLobbyHotbar(player);
        }
        StartClock.updateSigns();
        StartClock.openDateDiffrenceText = StartClock.NO_DATE_AVAILABLE;
        Info.sendLangImportantInfo("game-server-open");
    }

    public static void close() {
        config.set("game.open", false);
        Main.getPlugin().saveConfig();
        
        StartClock.updateSigns();
        StartClock.openDateDiffrenceText = StartClock.NO_DATE_AVAILABLE;
        for(Player all : Main.arena.getPlayers()) Main.joinServer(Server.LOBBY, all);
        for(Player all : Bukkit.getOnlinePlayers()) setLobbyHotbar(all);
        Info.sendImportantInfo("Der Spiel-Server wurde geschlossen!");
    }

    public static void startTimer() {
        setGameStatus(GameState.STARTING_MATCH);
        Info.sendLangImportantInfo("timer.gate-timer-start", "%sec%", "§b5");
        Info.showLangTitle("gate-open-countdown");
        for(Player all : Bukkit.getOnlinePlayers()) {
            all.getInventory().clear();
        }
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            int timer = 5;
            @Override
            public void run() {
                    if(timer <= 0) {
                        Info.showLangTitle("gate-open");
                        for(Player all : Bukkit.getOnlinePlayers()) all.playSound(all.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 5, 1);
                        
                        Bukkit.getScheduler().cancelTask(taskID);
                        startSpreadTimer();
                    } else {
                        Info.showLangTitle("get-ready-timer", "%sec%", timer + "");
                        for(Player all : Bukkit.getOnlinePlayers()) all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 5, 10);
                        
                        //Info.sendLangInfo("timer.gate-open-countdown", "%sec%", "§b" + timer);
                    }
                    timer--;
                }
        }, 0, 20);
    }

    public static void startSpreadTimer() {
        boolean isMatch = state().equals(GameState.STARTING_MATCH);
        if(isMatch) {
            MapManager.ifNotloadMap(GameMap.GAME);
            GateManager.setGateActive(true);
            Info.sendLangImportantInfo("timer.spread-timer-start", "%sec%", "§b10");
        } else Info.sendLangInfo("rematch.tutorial");
        
        
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            int timer = 10;
            @Override
            public void run() {
                if(timer <= 0) {
                    Info.sendLangImportantInfo("timer.game-start");
                    Bukkit.getScheduler().cancelTask(taskID);
                    Info.showLangTitle("fire-at-will");
                    for(Player all : Bukkit.getOnlinePlayers()) {
                        all.playSound(all.getLocation(), Sound.EVENT_RAID_HORN, 10, 1);
                    }
                    if(isMatch) beginRound();
                    else {
                        List<Location> poses = Main.parseList(config.getString("rematch-spawn.barrier-box"), Main.arena);
                        replaceBocks(poses.get(0), poses.get(1), Material.BARRIER, Material.AIR);

                        Instant gameEndTimestamp = Instant.now().plus(ROUND_LENGHT, ChronoUnit.MINUTES);
                        GameStats.set(GameStat.GAME_END_TIMESTAMP, gameEndTimestamp.toEpochMilli());
                        GameStats.set(GameStat.STATE, GameState.RUNNING_REMATCH);
                    }
                    
                } 
                else if(timer <= 10) {
                    if(timer <= 4) {
                        Info.showLangTitle("get-ready-start", "%sec%", "" + timer);
                        //Info.sendLangInfo("timer.game-start-countdown", "%sec%", "§b" + timer);
                    } 
                    for(Player all : Bukkit.getOnlinePlayers()) {
                        if(timer > 4) all.sendActionBar(Component.text("§b§o" + timer));
                        all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 5, 10);
                    }
                }
                timer--;
            }
        }, 0, 20);
    }

    public static void replaceBocks(Location p1, Location p2, Material replace, Material target) {
        World world = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());

        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());

        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for(int x = minX; x <= maxX; x++) {
            for(int y = minY; y <= maxY; y++) {
                for(int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if(block.getType() != replace) continue;
                    block.setType(target);
                }
            }
        }
    }
    
    public static void beginRound() {
        setChunksForceLoaded(true);
        MapManager.ifNotloadMap(GameMap.GAME);
        BossBarManager.setVisible(true);
        for(Team team : Team.values()) setNewTeamRespawnPoint(team);
        for(Player all : Main.arena.getPlayers()) BossBarManager.addPlayer(all);
        GameRepeat.start();
        Map<String, Team> livingPlayers = new HashMap<>();
        for(Map.Entry<String, Team> map : TeamManager.getTeamsPlayer().entrySet()) {
            Team team = map.getValue();
            Player player = Bukkit.getPlayer(UUID.fromString(map.getKey()));
            if(player == null) {
                Info.sendLangImportantInfo("left-player.game-start", "%team%", team.getChatColor() + team.getName());
                addLeftPlayer(UUID.fromString(map.getKey()), 3);
                return;
            }
            StatusManger.setPlayerStatus(Status.PLAYING, player);
            livingPlayers.put(map.getKey(), team);
            player.getInventory().clear();
            TeamManager.setTeamChestPlate(player);
            ItemStack snowball = Game.snowball;
            snowball.setAmount(2);
            player.getInventory().addItem(snowball);
            player.setHealthScale(6);
        }
        setLivingPlayers(livingPlayers);

        Instant gameEndTimestamp = Instant.now().plus(ROUND_LENGHT, ChronoUnit.MINUTES);
        GameStats.set(GameStat.GAME_END_TIMESTAMP, gameEndTimestamp.toEpochMilli());

        // Hearts Compensation
        int teamRedSize = TeamManager.getTeamPlayers(Team.RED).size();
        int teamBlueSize = TeamManager.getTeamPlayers(Team.BLUE).size();

        if((teamBlueSize + teamRedSize) <= 1) throw new IllegalCallerException("1 Player Game not allowed.");
        
        int teamSizeDiffence = teamBlueSize - teamRedSize;
        if(teamSizeDiffence != 0) {
            Team derpivedTeam = teamSizeDiffence < 0 ? Team.BLUE : Team.RED;
            
            List<Player> availableCompensationPlayers = TeamManager.getTeamPlayers(derpivedTeam).stream()
                .map(uuid -> Bukkit.getPlayer(UUID.fromString(uuid)))
                .collect(Collectors.toList());

            teamSizeDiffence = Math.abs(teamSizeDiffence);
            
            while (teamSizeDiffence > 0) {
            List<Player> compensationPlayersPool = availableCompensationPlayers;
                while(!compensationPlayersPool.isEmpty() && teamSizeDiffence > 0) {
                    Random random = new Random();
                    int bound = compensationPlayersPool.size()-1;
                    int playerIndex = bound <= 0 ? 0 : random.nextInt(bound);
                    
                    Player compensationPlayer = compensationPlayersPool.get(playerIndex);
                    compensationPlayersPool.remove(playerIndex);
                    compensationPlayer.setHealthScale(compensationPlayer.getHealthScale()+(3*2));
                    
                    teamSizeDiffence--;
                } 
            }
        }
        Game.updateTeamHearts();
        Game.setGameStatus(GameState.RUNNING_MATCH);

        List<Location> poses = Main.parseList(config.getString("spawn-points.respawn-points.barrier"));
        replaceBocks(poses.get(0), poses.get(1), Material.BARRIER, Material.AIR);
    }

    public static void nextRound(Team wonTeam) {
        // Splitting Teams
        Info.sendLangInfo("event.team-splitting");
        for(Map.Entry<String, Team> map : TeamManager.getTeamsPlayer().entrySet()) {
            if(map.getValue() == wonTeam) {
                Player player = Bukkit.getPlayer(UUID.fromString(map.getKey()));
                player.teleport(wonTeam.getTeamSpawnLocation());
                loadLobbyInv(player);
                continue;
            }
        }
        TeamManager.splitTeam(wonTeam);
    }

    public static void endRound() {
        setGameStatus(GameState.WAITING);

        // Recount Hearts
        updateTeamHearts();
        int blueHeats = getTeamHearts(Team.BLUE);
        int redHeats = getTeamHearts(Team.RED);

        // Determine Games Outcome
        if(redHeats == blueHeats) {
            Info.showLangTitle("rematch");
            Info.sendLangImportantInfo("rematch.start");
            startReMatch();
        }
        else {
            resetRound();
            Team winningTeam = (blueHeats > redHeats) ? Team.BLUE : Team.RED;
            if(TeamManager.getTeamPlayers(winningTeam).size() <= 1) {
                winner(Bukkit.getPlayer(UUID.fromString(TeamManager.getTeamPlayers(winningTeam).get(0))));
            } else {
                Info.sendLangImportantInfo("event.team-won", "%team%", "@team." + winningTeam.getId());
                nextRound(winningTeam);
            } 
        }
    }

    public static void resetRound() {
        MapManager.loadMap(GameMap.GAME);
        Bukkit.getScheduler().cancelTask(taskID);
        Bukkit.getScheduler().cancelTask(taskID2);
        BossBarManager.setVisible(false);
        BossBarManager.removeAll();
        GateManager.setGateActive(false);
        for(Player all : Main.arena.getPlayers()) {
            if(!TeamManager.getTeamsPlayer().containsKey(all.getUniqueId().toString())) continue;
            all.getInventory().clear();
            TeamManager.setTeamChestPlate(all);
            all.setHealthScale(20);
        }
        setLivingPlayers(new HashMap<>());
        setTeamHearts(new HashMap<>());
        GameStats.set(GameStat.GAME_END_TIMESTAMP, GameStat.GAME_END_TIMESTAMP.getDefaultValue());
        Main.clearDroppedItems();
        GameStats.set(GameStat.NEXT_RESPAWN_POINT, new HashMap<>());
        setChunksForceLoaded(false);
        clearLeftPlayer();
        setGameStatus(GameState.WAITING);
    }
    
    public static void hardReset() {
        resetRound();
        setGameStatus(GameState.CLOSED);
        for(Team team : Team.values()) TeamManager.clearTeam(team);
    }

    public static void pause() {
        Info.sendLangImportantInfo("paused");
        beforePause = state();
        setGameStatus(GameState.PAUSED);
    }
  
    public static void resume() {
        setGameStatus(beforePause);
        Info.sendLangImportantInfo("game.game-state-change", "%state%", "@state.resumed");
    }
    
    public static void winner(Player player) {
        resetRound();
        MapManager.loadMap(GameMap.WINNER);
        Info.sendLangImportantInfo("event.player-won", "%player%", "§6" + player.getName());
        for(Player all : Main.arena.getPlayers()) {
            Info.showLangTitle("player-won", "%player%", "" + player.getName());
            all.playSound(all.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 5, 1);
            if(all == player) continue;
            StatusManger.setPlayerStatus(Status.WATCHING, all);
        }
        StatusManger.setPlayerStatus(Status.WON, player);
        player.teleport(Main.parseLocation(config.getString("spawn-points.winner"), Main.arena));
    }

    public static Location getRematchSpawnLocation(Player player) {
        return Main.parseLocation(config.getString("rematch-spawn." + TeamManager.getTeam(player).getId()), Main.arena);
    }

    public static void startReMatch() {
        Info.sendLangImportantInfo("event.game-change-draw");

        ItemStack stick = new ItemBuilder(Material.STICK).addEnchant(Enchantment.KNOCKBACK, 2).setUnDroppable().build();

        for(Player all : Main.arena.getPlayers()) {
            if(getLivingPlayers().containsKey(all.getUniqueId().toString())) {
                //Downset Hearts
                int currentHearts = (int) all.getHealthScale();
                int newHearts = ((currentHearts+5)/6)*2;
                all.setHealthScale((double) newHearts);

                Game.updateTeamHearts();

                //Teleport on Platform
                Location loc = getRematchSpawnLocation(all);
                all.teleport(loc); 
                all.getInventory().remove(Material.SNOWBALL);

                //Give everyone Stick
                all.getInventory().setItem(0, stick);
            } 
        }
        MapManager.loadMap(GameMap.REMATCH);
        setGameStatus(GameState.STARTING_REMATCH);
        startSpreadTimer();
    }

    public static void setServerHotbar(Server server, Player player) {
        if(server == Server.EVENT_SERVER) setGameServerHotbar(player);
        if(server == Server.LOBBY) setLobbyHotbar(player);
    }

    public static void setGameServerHotbar(Player player) {
        Inventory inv = player.getInventory();
        UUID uuid = player.getUniqueId();
        for(int slot = 0; slot < 9; slot++) inv.setItem(slot, new ItemStack(Material.AIR));
        ItemStack joinTeam = new ItemBuilder(Material.BOOK).setLangNameDescriptionTag("join-team", uuid).setUnmovable().build();
        ItemStack cameraViews = new ItemBuilder(Material.SPYGLASS).setLangNameDescriptionTag("open-camera-views", uuid).setUnmovable().build();
        ItemStack backToLobby = new ItemBuilder(Material.BEACON).setLangNameDescriptionTag("join-lobby", uuid).setUnmovable().build();
        
        inv.setItem(8, backToLobby);
        inv.setItem(7, LanguageManager.getLanguageChangeItem(uuid));
        inv.setItem(0, joinTeam);
        inv.setItem(2, cameraViews);
        player.getInventory().setHeldItemSlot(1);
        player.updateInventory();
    }

    public static void setLobbyHotbar(Player player) {
        Inventory inv = player.getInventory();
        UUID uuid = player.getUniqueId();
        for(int slot = 0; slot < 9; slot++) inv.setItem(slot, new ItemStack(Material.AIR));
        ItemStack joinParkour = new ItemBuilder(Material.DARK_OAK_SLAB).setLangNameDescriptionTag("join-parkour", uuid).setUnmovable().build();
        
        inv.setItem(7, LanguageManager.getLanguageChangeItem(uuid));
        inv.setItem(2, joinParkour);
        updateJoinGameItem(player);
        player.updateInventory();
    }

    public static ItemStack getStartClockItem(Player player) {
        UUID uuid = player.getUniqueId();

        ItemBuilder joinGameServerBuilder = new ItemBuilder(Material.CLOCK).setDisplayName(LanguageManager.getItemName("closed-game-server", uuid).replace("%time%", StartClock.openDateDiffrenceText)).setUnmovable();

        if(Game.isOpen())
            joinGameServerBuilder = joinGameServerBuilder.setMaterial(Material.SNOWBALL)
                    .setLangNameDescriptionTag("join-game-server", uuid);
        else if(StartClock.isTimerStarted) joinGameServerBuilder = joinGameServerBuilder.setLangDescription("closed-game-server-starting", uuid);
        else joinGameServerBuilder = joinGameServerBuilder.setLangDescription("closed-game-server", uuid);

        return joinGameServerBuilder.build();
    }

    public static void updateJoinGameItem(Player player) {
        player.getInventory().setItem(0, getStartClockItem(player));
        player.updateInventory();
    }

    public static void deadMode(Player player) {
        StatusManger.setPlayerStatus(Status.DEAD, player);

        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());
        removeFromLivingPlayers(player.getUniqueId().toString());
        
        updateTeamHearts();

        setViewer(player);
        player.teleport(Main.parseLocation(Main.getPlugin().getConfig().getString("spawn-points.dead." + team.getId()), Main.arena));
        
        if(Game.getLivingPlayers(team).size() <= 0) endRound();
    }

    public static void openCameraViews(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§7Kamera-Aussichten"));
        
        UUID uuid = player.getUniqueId();

        ItemBuilder scull = new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner("2f8bd35f-0ccb-46c6-8321-6e15abe95c93")
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ0MjJhODJjODk5YTljMTQ1NDM4NGQzMmNjNTRjNGFlN2ExYzRkNzI0MzBlNmU0NDZkNTNiOGIzODVlMzMwIn19fQ==");

        inv.addItem(scull.setLangNameDescriptionTag("use-camera-up", uuid).build());
        inv.addItem(scull.setLangNameDescriptionTag("use-camera-blue-side", uuid).build());
        inv.addItem(scull.setLangNameDescriptionTag("use-camera-red-side", uuid).build());
        player.openInventory(inv);
    }

    public static boolean isOpen() { return config.getBoolean("game.open"); }

    public static void updateTeamHearts() {
        for(Team team : Team.values()) {
            int hearts = 0;
            for(String uuid : getLivingPlayers(team)) {
                Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                if (player == null) continue;
                hearts += player.getHealthScale();
            }
            setTeamHearts(hearts, team);
        }
    }

    public static Map<Team, Integer> getTeamHearts() {
        return GameStats.get(GameStat.TEAM_HEARTS);
    }

    public static int getTeamHearts(Team team) {
        return getTeamHearts().get(team);
    }

    public static void setTeamHearts(int hearts, Team team) {
        Map<Team, Integer> teamHearts = getTeamHearts();
        teamHearts.put(team, hearts);
        setTeamHearts(teamHearts);
    }

    public static void setTeamHearts(Map<Team, Integer> teamHearts) {
        GameStats.set(GameStat.TEAM_HEARTS, teamHearts);
    }

    public static Map<String, Team> getLivingPlayers() {
        return GameStats.get(GameStat.LIVING_PLAYERS);
    }

    public static void setLivingPlayers(Map<String, Team> livingPlayers) {
        GameStats.set(GameStat.LIVING_PLAYERS, livingPlayers);
    }

    public static void addToLivingPlayers(String uuid) {
        Map<String, Team> livingPlayers = getLivingPlayers();
        livingPlayers.put(uuid, TeamManager.getTeam(uuid));
        setLivingPlayers(livingPlayers);
        updateTeamHearts();
    }
    
    public static void removeFromLivingPlayers(String uuid) {
        Map<String, Team> livingPlayers = getLivingPlayers();
        livingPlayers.remove(uuid);
        setLivingPlayers(livingPlayers);
    }

    public static List<String> getLivingPlayers(Team team) {
        List<String> uuids = new ArrayList<>();
        for(Map.Entry<String, Team> entry : getLivingPlayers().entrySet()) {
            if(!entry.getValue().equals(team)) continue;
            uuids.add(entry.getKey());
        }
        return uuids;
    }

    public static void setGameStatus(GameState gameStat) {
        GameStats.set(GameStat.STATE, gameStat);
    }

    public static GameState state() {
        return GameStats.get(GameStat.STATE);
    }

    public static boolean isRunning() {
        return (Game.state().equals(GameState.RUNNING_MATCH) || Game.state().equals(GameState.RUNNING_REMATCH));
    }

    public static void dropBonusSnowball(Team team, int amount) {
        List<Location> locs = Main.parseList(config.getString("spawn-points.snowball." + team.getId()));
        Block dropperBlock = locs.get(0).getBlock();
        Block lampBlock = locs.get(1).getBlock();

        if(dropperBlock.getType() != Material.DROPPER) throw new IllegalArgumentException("Cords have to refer to DROPPER.");
        if(lampBlock.getType() != Material.REDSTONE_LAMP) throw new IllegalArgumentException("Cords have to refer to REDSTONE_LAMP.");

        Dropper dropper = (Dropper) dropperBlock.getState();
 
        new BukkitRunnable() {
            int count = 0;

            public void run() {
                if(count >= amount) {
                    this.cancel();
                    return;
                } 
                dropper.getInventory().addItem(Game.snowball);
                dropper.drop();
                dropper.update();
                setLampLit(lampBlock, true);

                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                    setLampLit(lampBlock, false);
                }, 5L);
                
                count++;
            }
        }.runTaskTimer(Main.getPlugin(), 0L, 10L);
    }
    
    public record HandoverContext(String proxyUuid, int playerHearts, int proxyHearts) {}
    
    public static void clearLeftPlayer() {
        Map<String, HandoverContext> leftPlayersMap = getLeftPlayersMap();
        leftPlayersMap.clear();
        GameStats.set(GameStat.LEFT_TEAM_PLAYERS, leftPlayersMap);
    }

    public static void addLeftPlayer(Player player) {
        addLeftPlayer(player.getUniqueId(), player.getHealthScale());
    }

    public static void addLeftPlayer(UUID playerUUID, double playerHearth) {
        Player proxyPlayer = pickProxyPlayer(TeamManager.getTeam(playerUUID.toString()));

        Info.sendInfo("ProxyPlayer: " + proxyPlayer);

        String uuid = null;
        Integer proxyHearts = 0;
        if(proxyPlayer == null) pause();
        else {
            uuid = proxyPlayer.getUniqueId().toString();
            proxyHearts = (int) proxyPlayer.getHealthScale();
        }

        Map<String, HandoverContext> leftPlayersMap = new HashMap<>();
        HandoverContext handoverContext = new HandoverContext(uuid, (int) playerHearth, proxyHearts);

        leftPlayersMap.put(playerUUID.toString(), handoverContext);
        GameStats.set(GameStat.LEFT_TEAM_PLAYERS, leftPlayersMap);

        if(proxyPlayer == null) return;

        double newProxyHearts = proxyPlayer.getHealthScale()+playerHearth;

        proxyPlayer.setHealthScale(newProxyHearts);
    }

    public static boolean isProxyPlayer(String uuid) {
        for(Entry<String, HandoverContext> entry : getLeftPlayersMap().entrySet()) if(entry.getValue().proxyUuid.equals(uuid)) return true;
        return false;
    }

    private static Player pickProxyPlayer(Team team) {
        Map<String, Integer> hearts = new HashMap<>();
        for(String livingUuid : Game.getLivingPlayers(team)) {
            if(isProxyPlayer(livingUuid)) continue;
            Player player = Bukkit.getPlayer(UUID.fromString(livingUuid));
            if(player == null) throw new IllegalArgumentException("Player null in Living Players List. Please Contact the developer.");
            hearts.put(livingUuid, (int) player.getHealthScale());
        }

        if(hearts.isEmpty()) return null;

        Integer minHearts = hearts.values().stream().min(Integer::compare).orElse(null);
        if(minHearts == null) throw new IllegalArgumentException("No hearts found!");

        List<String> lowPlayers = hearts.entrySet().stream()
            .filter(entry -> entry.getValue().equals(minHearts))
            .map(Map.Entry::getKey).toList();

        return Bukkit.getPlayer(UUID.fromString(lowPlayers.stream().findAny().orElse(null)));
    }

    public static void removeLeftPlayer(Player player) {
        Map<String, HandoverContext> leftPlayersMap = getLeftPlayersMap();
        leftPlayersMap.remove(player.getUniqueId().toString());
        GameStats.set(GameStat.LEFT_TEAM_PLAYERS, leftPlayersMap);
    }

    public static Map<String, HandoverContext> getLeftPlayersMap() {
        return GameStats.get(GameStat.LEFT_TEAM_PLAYERS);
    }

    public static List<String> getLeftPlayers() {
        return new ArrayList<>(getLeftPlayersMap().keySet());
    }

    public static HandoverContext getLeftPlayerHandoverContext(UUID uuid) {
        return getLeftPlayersMap().get(uuid.toString());
    }

    public static void setChunksForceLoaded(boolean loaded) {
        for(Chunk chunk : Main.getForceLoadChunks()) chunk.setForceLoaded(loaded);
    }

    public static List<Location> getRespawnPoints(Team team) {
        return Main.parseList(config.getString("spawn-points.respawn-points." + team.getId()));
    }

    public static void setNewTeamRespawnPoint(Team team) {
        Map<Team, Integer> nextTeamRespawnPoints = GameStats.get(GameStat.NEXT_RESPAWN_POINT);
        Random random = new Random();

        int oldLamp = nextTeamRespawnPoints.getOrDefault(team, -1);
                
        int newLamp = random.nextInt(getRespawnPoints(team).size()-1);
        if(newLamp>= oldLamp) newLamp++;
        nextTeamRespawnPoints.put(team, newLamp);
        GameStats.set(GameStat.NEXT_RESPAWN_POINT, nextTeamRespawnPoints);
        setLampLit(getRespawnPoints(team).get(newLamp).getBlock(), true);

        if(oldLamp >= 0 && oldLamp != newLamp) Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () ->  {
            if(getNextTeamRespawnPointId(team) != oldLamp) setLampLit(getRespawnPoints(team).get(oldLamp).getBlock(), false);
        }, 20L);
    }

    public static void setLampLit(Block lamp, boolean lit) {
        Lightable lightable = (Lightable) lamp.getBlockData();
        lightable.setLit(lit);
        lamp.setBlockData(lightable);
    }

    public static Integer getNextTeamRespawnPointId(Team team) {
        return ((Map<Team, Integer>) GameStats.get(GameStat.NEXT_RESPAWN_POINT)).get(team);
    }

    public static Location getNextTeamRespawnPoint(Team team) {
        return getRespawnPoints(team).get(getNextTeamRespawnPointId(team));
    }

    public static void respawnPlayer(Player player) {
        Team team = TeamManager.getTeam(player);
        Location loc = getNextTeamRespawnPoint(team);
        if(getNextTeamRespawnPoint(team) == null) setNewTeamRespawnPoint(team);
        loc.setY(loc.getY()+1);
        loc.add(0.5, 0, 0.5);
        player.teleport(loc);
        Main.arena.spawnParticle(Particle.CLOUD, player.getLocation(), 150, 0, 1, 0, 1);
        setNewTeamRespawnPoint(team);
    }

    /*public static String decoeSekToMinSek(int sek) {
        int min = sek/60;
        int mintesInSek = min*60;
        int newSek = sek-mintesInSek;

        return String.format("%02d:%02d", min, newSek);
    }*/

}
