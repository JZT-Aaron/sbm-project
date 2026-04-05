package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.DemoManger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Repeat {

    public static int taskID;
    public static int taskID2 = -1;

    public static List<UUID> movingPlayers = new ArrayList<>();

    public static void start() {
        if(Main.isDemo) {
            taskID2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if(Main.isDemo && DemoManger.timestamp != null && DemoManger.timestamp.isBefore(Instant.now()) && Bukkit.getOnlinePlayers().size() <= 0) {
                        DemoManger.closeServer();
                    }
                } 
            }, 0, 1200);

        }
        
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Runnable() {
            int run = 0;

            @Override
            public void run() {
                if(Game.isSnowing) {
                    run++;
                    if(run == 4) run = 0;
                    for(UUID uuid : PlayerStats.getSnowPlayers()) {
                        if(run == 3 || (run != 0 && !movingPlayers.contains(uuid))) continue;
                        Player all = Bukkit.getPlayer(uuid);
                        if(all == null) {
                            PlayerStats.setOffline(uuid);
                            continue;
                        }

                        if(all.getLocation().getBlock().getLightFromSky() < 3) continue;
                        Location eyeLocation = all.getEyeLocation();
                        Vector direction = eyeLocation.getDirection();
                        direction.setY(0);
                        if(direction.lengthSquared() > 0) direction.normalize();
                        
                        Location focusPoint = eyeLocation.clone().add(direction.multiply(5)).add(0, 3, 0);

                        if(focusPoint.getBlock().getLightFromSky() < 10) continue;

                        for (int i = 0; i < 3; i++) {
                            double xOffset = (Math.random() * 20) - 10;
                            double yOffset = (Math.random() * 10);
                            double zOffset = (Math.random() * 20) - 10;
                            
                            Location particleLoc = focusPoint.clone().add(xOffset, yOffset, zOffset);
                            if(particleLoc.getBlock().getLightFromSky() < 15) continue;
                            all.spawnParticle(Particle.FIREWORK, particleLoc, 50, 2, 5, 2, 0.01);
                        }
                    }
                }
            }
        }, 0, 1);
    }

    public static void stop() {
        Bukkit.getScheduler().cancelTask(taskID);
        if(taskID2 != -1) {
            Bukkit.getScheduler().cancelTask(taskID2);
            taskID2 = -1;
        } 
    }

}
