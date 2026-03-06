package de.dachente.sbm.utils;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.dachente.sbm.main.Main;
import net.kyori.adventure.text.Component;

public class GameRepeat {
    public static int taskID;

    public static void start() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if(Game.isRunning()) {
                    for(Player all : Bukkit.getOnlinePlayers()) 
                        all.sendActionBar(Component.text("§9§l♥ " + (Game.getTeamHearts(Team.BLUE)/2) + " §7- §c§l♥ " + (Game.getTeamHearts(Team.RED)/2)));
                    
                    Long gameEndMilli = GameStats.get(GameStat.GAME_END_TIMESTAMP);
                    if(gameEndMilli != null) {
                        Duration duration = Duration.between(Instant.now(), Instant.ofEpochMilli(gameEndMilli));
                        long minutes = duration.toMinutes();
                        long seconds = duration.minusMinutes(minutes).getSeconds();
                        String timeText = String.format("%02d:%02d", minutes, seconds);
                        Game.bossBar.setTitle("§7§l" + timeText);
                        if(duration.isZero() || duration.isNegative()) {
                            stop();
                            GameStats.set(GameStat.GAME_END_TIMESTAMP, GameStat.GAME_END_TIMESTAMP.getDefaultValue());
                            Game.endRound();
                        }
                    }
                }
                    
            }   
        }, 0, 20);
    }

    public static void stop() {
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
