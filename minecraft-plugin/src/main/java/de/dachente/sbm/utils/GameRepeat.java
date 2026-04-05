package de.dachente.sbm.utils;

import static de.dachente.sbm.managers.LanguageManager.getText;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

public class GameRepeat {
    public static int taskID;

    public static void start() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if(Game.state().equals(GameState.PAUSED)) {
                    for(Player all : Main.arena.getPlayers()) all.sendActionBar(Component.text(getText("state.paused", all.getUniqueId())));
                    return;
                }
                if(Game.isRunning()) {
                    for(Player all : Main.arena.getPlayers()) 
                        all.sendActionBar(Component.text("§9§l♥ " + (Game.getTeamHearts(Team.BLUE)/2) + " §7- §c§l♥ " + (Game.getTeamHearts(Team.RED)/2)));
                    
                    Long gameEndMilli = GameStats.get(GameStat.GAME_END_TIMESTAMP);
                    String timeText = "--:--";
                    if(gameEndMilli != null) {
                        Duration duration = Duration.between(Instant.now(), Instant.ofEpochMilli(gameEndMilli));
                        long minutes = duration.toMinutes();
                        long seconds = duration.minusMinutes(minutes).getSeconds();
                        timeText = String.format("%02d:%02d", minutes, seconds);
                        if(duration.isZero() || duration.isNegative()) {
                            GameStats.set(GameStat.GAME_END_TIMESTAMP, GameStat.GAME_END_TIMESTAMP.getDefaultValue());
                            timeText = "--:--";
                            updateBassBar(timeText);
                            Game.endRound();
                        }
                        updateBassBar(timeText);
                    } else if(!Game.isRunning()) {
                        stop();
                    } 
                }
                    
            }   
        }, 0, 20);
    }

    private static void updateBassBar(String timeText) {
        BossBarManager.setTitle("left-time", "%time%", "§f§l" + timeText);
    }

    public static void stop() {
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
