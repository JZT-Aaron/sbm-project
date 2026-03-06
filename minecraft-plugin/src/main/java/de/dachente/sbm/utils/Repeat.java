package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class Repeat {

    public static int taskID;

    public static void start() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if(Game.isSnowing) {
                    for(Player all : Bukkit.getOnlinePlayers()) all.spawnParticle(Particle.FIREWORK, all.getLocation(), 500, 7, 7, 7, 0.01);
                }
            }
        }, 0, 15);
    }

    public static void stop() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

}
