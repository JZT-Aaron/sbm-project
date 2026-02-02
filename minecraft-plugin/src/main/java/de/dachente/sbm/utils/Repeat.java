package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Repeat {

    public static int taskID;

    public static void start() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                for(Player all : Bukkit.getOnlinePlayers()) {
                    if(Game.isSnowing) {
                        all.spawnParticle(Particle.FIREWORK, all.getLocation(),500, 7, 7, 7, 0.01);
                    }
                    if(Game.isStarted) {
                        int blueHeats = 0;
                        int redHeats = 0;
                        for(String uuid : Game.livingPlayersTeamBlue) {
                            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                            if (player == null) continue;
                            blueHeats += player.getHealthScale();
                        }
                        for(String uuid : Game.livingPlayersTeamRed) {
                            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                            if (player == null) continue;
                            redHeats += player.getHealthScale();
                        }
                        all.sendActionBar(Component.text("§9§l♥ " + (blueHeats/2) + " §7- §c§l♥ " + (redHeats/2)));
                    }
                }
            }
        }, 0, 15);
    }

    public static void stop() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

}
