package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class StartClock {

    static int taskID;

    static FileConfiguration config = Main.getPlugin().getConfig();
    static final String NAME = "Event Server";
    public static boolean isStarted = false;

    public static final String NO_DATE_AVAILABLE = "§7§oN/A";
    private final static String SIGN_LINE = "§f§l--------------";
    public static String openDateDiffrenceText = NO_DATE_AVAILABLE;

    public static List<Block> signs = new ArrayList<>(List.of(
                Main.lobby.getBlockAt(43, -5, 4),
                Main.lobby.getBlockAt(43, -5, 6),
                Main.lobby.getBlockAt(-4, 1, 49),
                Main.lobby.getBlockAt(-45, 0, 8),
                Main.lobby.getBlockAt(-5, 1, -33),
                Main.lobby.getBlockAt(-13, 0, 6),
                Main.lobby.getBlockAt(-13, 0, 2)));

    public static void stop() {
        isStarted = false;
        Bukkit.getScheduler().cancelTask(taskID);
    }

    public static void start() {
        isStarted = true;

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

                int y = period.getYears();
                int M = period.getMonths();
                int d = period.getDays();
                int h = duration.toHoursPart();
                int m = duration.toMinutesPart();
                int s = duration.toSecondsPart();

                StringBuilder timeBuilder = new StringBuilder();
                if(duration.toHours() < 24) d = 0;

                if (y > 0) timeBuilder.append(y).append(" J. ");
                if (M > 0) timeBuilder.append(M).append(" M. ");
                timeBuilder.append(d).append(timeBuilder.length() < 11 ? " Tage " : " T. ");
                

                String dateDiffrence = timeBuilder.toString().trim();
                dateDiffrence = dateDiffrence.isEmpty() ? "§7§oHeute!!" : "§7§o" + dateDiffrence;

                String timeDiffrence = String.format("%02d:%02d:%02d", h, m, s);

                openDateDiffrenceText = (dateDiffrence + " ◆ " + timeDiffrence).trim();

                for(Player all : Main.lobby.getPlayers()) Game.setLobbyHotbar(all);
                
                setSignsText(
                    "§a" + NAME, 
                    "§f§l---§7Öffnung:§f§l---", 
                    dateDiffrence, 
                    "§7§o" + timeDiffrence);

                
                boolean isOpeningToday = period.isZero() || period.isNegative();

                if(now.isAfter(targetDateTime) || now.isEqual(targetDateTime)) {
                    Game.open();
                    stop();
                }

                if (isOpeningToday && isBetween(h, 1, 9) && m+s == 0) {
                    Game.sendInfo(NAME + " öffnet in §7" + h + " §oStunden!");
                }
                if (isOpeningToday && h+s == 0 && m != 0) {
                    switch (m) {
                        case 60: case 50: case 40: case 30: case 20: case 10: case 5: case 3: case 2:
                            Game.sendInfo(NAME + " öffnet in §7" + m + " §oMinuten!");
                            break;
                        case 1:
                            Game.sendInfo(NAME + " öffnet in §7einer §oMinute!");
                            break;
                        default:
                            return;
                    }

                }
                if (isOpeningToday && h+m == 0 && s != 0) {
                    switch (s) {
                        case 60: case 50: case 40: case 30: case 20: case 10: case 5:
                            Game.sendInfo(NAME + " öffnet in §7" + s + " §oSekunden!");
                            break;
                        case 3: case 2:
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                Game.showTitle("§c§l" + s, all);
                                Game.sendImportantInfo(NAME + " öffnet in §7" + s + " §oSekunden!");
                            }
                            break;
                        case 1:
                            for (Player all : Bukkit.getOnlinePlayers())
                                Game.showTitle("§c§l" + s, all);
                            Game.sendImportantInfo(NAME + " öffnet in §7einer §oSekunden!");
                            break;
                        default:
                            return;
                    }

                }
            }
        },0,20);
    }

    public static void setSignsInfo(String info) {
        setSignsText(
            SIGN_LINE, 
            "§a" + NAME, 
            info, 
            SIGN_LINE);
    }

    public static void setSignsText(String line1, String line2, String line3, String line4) {
        for (Block signBlock : signs) {
            Sign sign = (Sign) signBlock.getState();
            var frontSide = sign.getSide(Side.FRONT);

            frontSide.line(0, Component.text(line1));
            frontSide.line(1, Component.text(line2));
            frontSide.line(2, Component.text(line3));
            frontSide.line(3, Component.text(line4));

            sign.update();
        }
    }

    private static boolean isBetween(int input, int val1, int val2) {
        return val1 > val2 ? input >= val2 && input <= val1 : input >= val1 && input <= val2;
    }
}
