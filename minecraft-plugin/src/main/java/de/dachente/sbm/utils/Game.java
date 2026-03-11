package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Game {

    static FileConfiguration config = Main.getPlugin().getConfig();
    static int taskID;
    static int taskID2;

    public static boolean isSnowing = true;
    
    public static List<String> leftTeamPlayers = new ArrayList<>();
    public static List<ArmorStand> cameraPoints = new ArrayList<>();

    public static int blueHearts = 0;
    public static int redHearts = 0;

    private static final int ROUND_LENGHT = 1;

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
        if(TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) setTeamChestPlate(player);
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
        setGameStatus(GameState.STARTING);
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
        GateManager.setGateActive(true);
        Info.sendLangImportantInfo("timer.spread-timer-start", "%sec%", "§b10");
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
                    beginRound();
                    
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
    
    public static void beginRound() {
        BossBarManager.setVisible(true);
        for(Player all : Main.arena.getPlayers()) BossBarManager.addPlayer(all);
        GameRepeat.start();
        Map<String, Team> livingPlayers = new HashMap<>();
        for(Map.Entry<String, Team> map : TeamManager.getTeamsPlayer().entrySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(map.getKey()));
            StatusManger.setPlayerStatus(Status.PLAYING, player);
            if(player == null) return;
            livingPlayers.put(map.getKey(), map.getValue());
            player.getInventory().clear();
            ItemStack snowball = new ItemStack(Material.SNOWBALL);
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

        resetRound();

        // Determine Games Outcome
        if(redHeats == blueHeats) {
            Info.sendInfo("[DEBUG] End Round Determined: REMATCH");
        } 
        else {
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
        Bukkit.getScheduler().cancelTask(taskID);
        Bukkit.getScheduler().cancelTask(taskID2);
        BossBarManager.setVisible(false);
        BossBarManager.removeAll();
        GateManager.setGateActive(false);
        setLivingPlayers(new HashMap<>());
        setTeamHearts(new HashMap<>());
        GameStats.set(GameStat.GAME_END_TIMESTAMP, GameStat.GAME_END_TIMESTAMP.getDefaultValue());
        for(Entity entity : Main.arena.getEntities()) {
            if(!entity.getType().equals(EntityType.ITEM) ||
                    !(entity.getLocation().distance(new Location(Main.arena, 0.5, 0, 0)) <= 20)) continue;
            entity.remove();
        }
    }
    
    public static void hardReset() {
        resetRound();
        setGameStatus(GameState.CLOSED);
        for(Team team : Team.values()) TeamManager.clearTeam(team);
    }

    public static void winner(Player player) {
        Info.sendLangImportantInfo("event.player-won", "%player%", "§6" + player.getName());
        for(Player all : Main.arena.getPlayers()) {
            Info.showLangTitle("player-won", "%player%", "" + player.getName());
            all.playSound(all.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 5, 1);
            if(all == player) continue;
            Location l = all.getLocation();
            l.setZ(l.getZ()+86.5);
            all.teleport(l);
            StatusManger.setPlayerStatus(Status.WATCHING, all);
        }
        StatusManger.setPlayerStatus(Status.WON, player);
        player.teleport(new Location(Main.arena,0.5, 9, 86.5, 90, 0));
    }

    public static void startReMatch() {
        Info.sendLangImportantInfo("event.game-change-draw");
        for(Player all : Main.arena.getPlayers()) {
            if(getLivingPlayers().containsKey(all.getUniqueId().toString())) {
                Team team = TeamManager.getTeamsPlayer().get(all.getUniqueId().toString());
                if(team == Team.BLUE) {
                    all.teleport(new Location(Main.arena, 0.5, 2, -83.5, 180, 0));
                }
                if(team == Team.RED) {
                    all.teleport(new Location(Main.arena, 0.5, 2, -87.5, 0, 0));
                }
                all.getInventory().remove(Material.SNOWBALL);
            } else {
                Location playerLocation = all.getLocation();
                all.teleport(new Location(Main.arena, playerLocation.getX(), playerLocation.getY(), playerLocation.getZ()-85.5, playerLocation.getYaw(), playerLocation.getPitch()));
            }
        }
        setGameStatus(GameState.RUNNING_REMATCH);
    }

    public static void setTeamChestPlate(Player player) {
        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());
        setTeamChestPlate(team, player);
    }

    public static void setTeamChestPlate(Team team, Player player) {
        ItemStack teamChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta teamChestplateMeta = (LeatherArmorMeta) teamChestplate.getItemMeta();
        teamChestplateMeta.setColor(team.getColor());
        teamChestplateMeta.getPersistentDataContainer().set(Main.NO_MOVE, PersistentDataType.BYTE, (byte) 1);
        teamChestplateMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        teamChestplateMeta.addItemFlags(ItemFlag.HIDE_DYE);
        teamChestplate.setItemMeta(teamChestplateMeta);
        player.getInventory().setChestplate(teamChestplate);
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

    public static void removeFromLivingPlayers(String uuid) {
        Map<String, Team> livingPlayers = getLivingPlayers();
        livingPlayers.remove(uuid);
        GameStats.set(GameStat.LIVING_PLAYERS, livingPlayers);
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

    /*public static String decoeSekToMinSek(int sek) {
        int min = sek/60;
        int mintesInSek = min*60;
        int newSek = sek-mintesInSek;

        return String.format("%02d:%02d", min, newSek);
    }*/

}
