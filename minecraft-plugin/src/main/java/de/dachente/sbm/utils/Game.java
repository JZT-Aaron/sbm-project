package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;


import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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

import java.time.Duration;
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
    public static BossBar bossBar = Bukkit.createBossBar("§7§lLaden ...", BarColor.WHITE, BarStyle.SOLID);

    public static int blueHearts = 0;
    public static int redHearts = 0;

    private static final int ROUND_LENGHT = 1;

    public static void setSnowing(boolean snowing) {
        isSnowing = snowing;
    }

    public static void loadLobbyInv(Player player) {
        StatusManger.setPlayerStatus(Status.WAITING, player);

        UUID uuid = player.getUniqueId();

        ItemStack leaveTeam = new ItemBuilder(Material.RED_BED).setDisplayName("§7§lTeam verlassen").setTagData("leave-team")
                .setLore("§7Betätige um dein Team zu verlassen").build();
        player.getInventory().setItem(8, leaveTeam);
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
        StartClock.setSignsInfo("§a§oOffen");
        StartClock.openDateDiffrenceText = StartClock.NO_DATE_AVAILABLE;
        Info.sendImportantInfo("Der Spiel-Server hat jetzt geöffnet! Ihr könnt über das Menü oder mit /game-server joinen.");
    }

    public static void close() {
        config.set("game.open", false);
        Main.getPlugin().saveConfig();
        
        StartClock.setSignsInfo("§c§oGeschlossen");
        StartClock.openDateDiffrenceText = StartClock.NO_DATE_AVAILABLE;
        for(Player all : Main.arena.getPlayers()) all.teleport(Main.lobby.getSpawnLocation());
        for(Player all : Bukkit.getOnlinePlayers()) setLobbyHotbar(all);
        Info.sendImportantInfo("Der Spiel-Server wurde geschlossen!");
    }

    public static void startTimer() {
        setGameStatus(GameState.STARTING);
        Info.sendImportantInfo("§c§oDas Spiel startet in 5 sek!");
        for(Player all : Bukkit.getOnlinePlayers()) {
            Info.showTitle("§7Tore öffnen in: ", all);
        }
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            int timer = 5;
            @Override
            public void run() {
                    if(timer <= 0) {
                        for(Player all : Bukkit.getOnlinePlayers()) {
                            all.playSound(all.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 5, 1);
                            Info.showTitle("§7Tore offen! ", "Verteilt euch", all);
                        }
                        Bukkit.getScheduler().cancelTask(taskID);
                        startSpreadTimer();
                    } else {
                        for(Player all : Bukkit.getOnlinePlayers()) {
                            Info.showTitle("§7§o" + timer, "Macht euch bereit", all);
                            all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 5, 10);
                        }   
                        Info.sendInfo(timer + "");
                    }
                    timer--;
                }
        }, 0, 20);
    }

    public static void startSpreadTimer() {
        GateManager.setGateActive(true);
        Info.sendImportantInfo("§c§oDas Spiel startet! §7§oIhr hab 10 sek um euch auf dem Spielfeld zu verteilen!");
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            int timer = 10;
            @Override
            public void run() {
                if(timer <= 0) {
                    Info.sendImportantInfo("§c§oEs geht los!");
                    Bukkit.getScheduler().cancelTask(taskID);
                    for(Player all : Bukkit.getOnlinePlayers()) {
                        Info.showTitle("§7Start!", "§7Das Feuer ist eröffnet!", all);
                        all.playSound(all.getLocation(), Sound.EVENT_RAID_HORN, 10, 1);
                    }
                    
                    beginRound();
                    
                } 
                else if(timer <= 10) {
                    for(Player all : Bukkit.getOnlinePlayers()) {
                        if(timer <= 4) Info.showTitle("§7§o" + timer, "Seit Bereit", all);
                        else all.sendActionBar(Component.text("§7§o" + timer));
                        all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 5, 10);
                    }
                    if(timer <= 4) Info.sendInfo(timer + "");
                }
                timer--;
            }
        }, 0, 20);
    }
    
    public static void beginRound() {
        bossBar.setVisible(true);
        for(Player all : Main.arena.getPlayers()) bossBar.addPlayer(all);
        GameRepeat.start();
        Map<String, Team> livingPlayers = new HashMap<>();
        for(Map.Entry<String, Team> map : TeamManager.getTeamsPlayer().entrySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(map.getKey()));
            StatusManger.setPlayerStatus(Status.PLAYING, player);
            if(player == null) return;
            livingPlayers.put(map.getKey(), map.getValue());
            player.getInventory().remove(Material.RED_BED);
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
        Info.sendInfo("Das Team wird nun aufgeteilt.");
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
                Info.sendImportantInfo("Das Team " + winningTeam.getChatColor() + winningTeam.getName() + " §7§okommt weiter.");
                nextRound(winningTeam);
            } 
        }
    }

    public static void resetRound() {
        Bukkit.getScheduler().cancelTask(taskID);
        Bukkit.getScheduler().cancelTask(taskID2);
        Game.bossBar.setVisible(false);
        bossBar.removeAll();
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
        Info.sendImportantInfo("Der Spieler §6" + player.getName() + " §7§ohat die Schneeballschlacht gewonnen!");
        for(Player all : Main.arena.getPlayers()) {
            Info.showTitle("§6§l§n" + player.getName(), "§7§lhat gewonnen", all);
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
        Info.sendImportantInfo("Das Spielfeld wird wegen eines Unentschiedens geändert.");
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
        for(int slot = 0; slot < 9; slot++) inv.setItem(slot, new ItemStack(Material.AIR));
        ItemStack joinTeam = new ItemBuilder(Material.BOOK).setDisplayName("§7§lTeilnehmen §7§o(klick)").setTagData("join-team")
                .setLore("§7Betätige um am Wettkampf Teilzunehmen").build();
        ItemStack cameraViews = new ItemBuilder(Material.SPYGLASS).setDisplayName("§7§lKamera-Ausichten §7§o(klick)").setTagData("open-camera-views")
                .setLore("§7Betätige um die Auswahl zu verschiedenen Kamera-Ausichen zu wählen").build();
        ItemStack backToLobby = new ItemBuilder(Material.BEACON).setDisplayName("§7§lLobby §7§o(klick)").setTagData("join-lobby")
                .setLore("§7Betätige um zurück in die Lobby zu gelangen").build();
        inv.setItem(8, backToLobby);
        inv.setItem(4, joinTeam);
        inv.setItem(0, cameraViews);
        player.getInventory().setHeldItemSlot(3);
        player.updateInventory();
    }

    public static void setLobbyHotbar(Player player) {
        Inventory inv = player.getInventory();
        for(int slot = 0; slot < 9; slot++) inv.setItem(slot, new ItemStack(Material.AIR));
        ItemStack joinParkour = new ItemBuilder(Material.DARK_OAK_SLAB).setDisplayName("§7§lParkour §7§o(klick)").setTagData("join-parkour").setUnmovable()
                .setLore("§7Betätige um dich zum Startpunkt", "§7des Parkours zu teleportieren").build();
        
        ItemBuilder joinGameServerBuilder = new ItemBuilder(Material.CLOCK).setDisplayName("§c§lÖffnung: §7§o" + StartClock.openDateDiffrenceText).setUnmovable();;

        if(Game.isOpen()) {
            joinGameServerBuilder = joinGameServerBuilder.setMaterial(Material.SNOWBALL)
                    .setDisplayName("§7§lGame-Server §7§o(klick)")
                    .setTagData("join-game-server")
                    .setLore("§7Betätige um dem Game-Server beizutreten.");
        } else if(StartClock.isRoundGoing) joinGameServerBuilder = joinGameServerBuilder.setLore("§7Lies ab wann der Game-Server öffnet");
            else joinGameServerBuilder = joinGameServerBuilder.setLore("§7Die Öffnung ist nicht festgelegt.");

        ItemStack joinGameServer = joinGameServerBuilder.build();
        
        inv.setItem(2, joinGameServer);
        inv.setItem(6, joinParkour);
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
        
        inv.addItem(new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§f§lKamera Oben").setTagData("use-camera-up").setSkullOwner("2f8bd35f-0ccb-46c6-8321-6e15abe95c93")
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ0MjJhODJjODk5YTljMTQ1NDM4NGQzMmNjNTRjNGFlN2ExYzRkNzI0MzBlNmU0NDZkNTNiOGIzODVlMzMwIn19fQ==").build());
        inv.addItem(new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§f§lKamera Seite Rot").setTagData("use-camera-red-side").setSkullOwner("2f8bd35f-0ccb-46c6-8321-6e15abe95c93")
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ0MjJhODJjODk5YTljMTQ1NDM4NGQzMmNjNTRjNGFlN2ExYzRkNzI0MzBlNmU0NDZkNTNiOGIzODVlMzMwIn19fQ==").build());
        inv.addItem(new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§f§lKamera Seite Blau").setTagData("use-camera-blue-side").setSkullOwner("2f8bd35f-0ccb-46c6-8321-6e15abe95c93")
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ0MjJhODJjODk5YTljMTQ1NDM4NGQzMmNjNTRjNGFlN2ExYzRkNzI0MzBlNmU0NDZkNTNiOGIzODVlMzMwIn19fQ==").build());
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

        return null;
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
