package de.dachente.sbm.utils;

import de.dachente.sbm.listeners.MutliLangSignManager;
import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import static de.dachente.sbm.managers.LanguageManager.getText;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StartClock {

    static int taskID;

    static FileConfiguration config = Main.getPlugin().getConfig();
    public static boolean isTimerStarted = false;

    public static final String NO_DATE_AVAILABLE = "§7§oN/A";
    private final static String SIGN_LINE = "§f§l--------------";
    public static String openDateDiffrenceText = NO_DATE_AVAILABLE;


    private static int y;
    private static int M;
    private static int d;
    private static int h;
    private static int m;
    private static int s;

    public static List<Location> signs = new ArrayList<>();

    public static void stop() {
        isTimerStarted = false;
        Bukkit.getScheduler().cancelTask(taskID);
    }

    //TODO: Test if that Text System works.

    public static void start() {
        isTimerStarted = true;

        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                LocalDateTime targetDateTime = LocalDateTime.of(
                        config.getInt("start.clock.time.y"),
                        config.getInt("start.clock.time.M"),
                        config.getInt("start.clock.time.d"),
                        config.getInt("start.clock.time.h"),
                        config.getInt("start.clock.time.m"),
                        config.getInt("start.clock.time.s")
                );

                LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).truncatedTo(ChronoUnit.SECONDS);
                targetDateTime = targetDateTime.truncatedTo(ChronoUnit.SECONDS);

                Period period = Period.between(now.toLocalDate(), targetDateTime.toLocalDate());
                Duration duration = Duration.between(now, targetDateTime); 

                y = period.getYears();
                M = period.getMonths();
                d = period.getDays();
                h = duration.toHoursPart();
                m = duration.toMinutesPart();
                s = duration.toSecondsPart();

                if(duration.toHours() < 24) d = 0;

                for(Player all : Main.lobby.getPlayers()) {
                    updateTimeDisplay(all, false);
                } 
                
                boolean isOpeningToday = period.isZero() || period.isNegative();

                if(now.isAfter(targetDateTime) || now.isEqual(targetDateTime)) {
                    Game.open();
                    stop();
                }

                if (isOpeningToday && isBetween(h, 1, 9) && m+s == 0) {
                    Info.sendInfo(NAME + " öffnet in §7" + h + " §oStunden!");
                }
                if (isOpeningToday && h+s == 0 && m != 0) {
                    switch (m) {
                        case 60: case 50: case 40: case 30: case 20: case 10: case 5: case 3: case 2:
                            Info.sendInfo(NAME + " öffnet in §7" + m + " §oMinuten!");
                            break;
                        case 1:
                            Info.sendInfo(NAME + " öffnet in §7einer §oMinute!");
                            break;
                        default:
                            return;
                    }

                }
                if (isOpeningToday && h+m == 0 && s != 0) {
                    switch (s) {
                        case 60: case 50: case 40: case 30: case 20: case 10: case 5:
                            Info.sendInfo(NAME + " öffnet in §7" + s + " §oSekunden!");
                            break;
                        case 3: case 2:
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                Info.showTitle("§c§l" + s, all);
                                Info.sendImportantInfo(NAME + " öffnet in §7" + s + " §oSekunden!");
                            }
                            break;
                        case 1:
                            for (Player all : Bukkit.getOnlinePlayers())
                                Info.showTitle("§c§l" + s, all);
                            Info.sendImportantInfo(NAME + " öffnet in §7einer §oSekunden!");
                            break;
                        default:
                            return;
                    }

                }
            }
        },0,20);
    }

    private static void sendCountdownMessage(int time, String unitId) {
        String timeString = time + ""; 

        if(time == 1) {
            timeString = "@unit.one";
            unitId.replace("s", "");
        }
        Info.sendLangInfo("server-open-countdown", "%server%", "@instances.game-server", "%time%", timeString, "%unit%", "unit." + unitId);
    }

    public static void updateTimeDisplay(Player player, boolean ignoreDis) {
        StringBuilder timeBuilder = new StringBuilder();     

        if (y > 0) timeBuilder.append(y).append(" Y. ");
        if (M > 0) timeBuilder.append(M).append(" M. ");
        
        UUID uuid = player.getUniqueId();
        StringBuilder timeBuilderClone = new StringBuilder(timeBuilder.toString());
        if(d > 0) timeBuilderClone.append(d).append(timeBuilderClone.length() < 11 ? " "+ getText("info.time.days", uuid) +" " : " "+ getText("info.time.days-abb", uuid) +" ");
    
        String dateDiffrence = timeBuilderClone.toString().trim();
        dateDiffrence = dateDiffrence.isEmpty() ? "§7§o" + getText("info.time.today", uuid) : "§7§o" + dateDiffrence;

        String timeDiffrence = String.format("%02d:%02d:%02d", h, m, s);

        openDateDiffrenceText = (dateDiffrence + " ◆ " + timeDiffrence).trim();

        Game.updateJoinGameItem(player);
        
        setSignsText(player, ignoreDis,
        "§b" + getGameServerName(uuid), 
        "§f§l-- §7" + getText("state.opening", uuid) + "§f§l --", 
        dateDiffrence, 
        "§7§o" + timeDiffrence);
    }

    public static void setSignsInfo(String infoID, String color) {
        setSignsInfo(infoID, color, false);
    }

    public static void setSignsInfo(String infoID, String color, boolean ignoreDis) {
        for(Player all : Main.lobby.getPlayers()) {
            UUID uuid = all.getUniqueId();
            setSignsText(all, ignoreDis,
            SIGN_LINE, 
            "§b" + getGameServerName(uuid), 
            color + getText(infoID, uuid), 
            SIGN_LINE);
    }

    }

    private static String getGameServerName(UUID uuid) {
        return LanguageManager.getText("instances.game-server", uuid);
    }

    public static void setSignsText(Player player, boolean ignoreDis, String... lines) {   
        for (Location signBlockLoc : signs) {
            if(!ignoreDis && (!signBlockLoc.getWorld().equals(player.getWorld()) || signBlockLoc.distanceSquared(player.getLocation()) > 4096)) continue;
            MutliLangSignManager.sendSign(player, signBlockLoc, lines);
        }
    }

    public static void updateSigns() {
        for(Player all : Main.lobby.getPlayers()) updateSigns(all, false);
    }

    public static void updateSigns(Player player, boolean ignoreDis) {
        if(Game.isOpen()) StartClock.setSignsInfo("state.open", "§a§o", ignoreDis);
        else if(isTimerStarted) StartClock.updateTimeDisplay(player, ignoreDis);
        else StartClock.setSignsInfo("state.closed", "§c§o", ignoreDis);
    }

    private static boolean isBetween(int input, int val1, int val2) {
        return val1 > val2 ? input >= val2 && input <= val1 : input >= val1 && input <= val2;
    }
}
