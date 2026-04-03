package de.dachente.sbm.listeners;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;

public class SnowballFlyListener implements Listener {

    @EventHandler
    public void onFly(ProjectileLaunchEvent event) {
        if(!(event.getEntity() instanceof Snowball snowball) || !Game.isRunning()) return;
        new BukkitRunnable() {
            public void run() {
                if(snowball.isDead() || !snowball.isValid()) {
                    this.cancel();
                    return;
                }

                List<Location> fieldCorners = Main.parseList(Main.getPlugin().getConfig().getString("field-boundary"));
                if(fieldCorners.size() < 2) throw new IllegalArgumentException("Please use two Block Coordinates for field-boundary. Format: x,y,z;x,y,z");

                BoundingBox boundingBox = BoundingBox.of(fieldCorners.get(0).getBlock(), fieldCorners.get(1).getBlock());
                Location loc = snowball.getLocation();               
                if(!boundingBox.contains(loc.getBlock().getLocation().toVector())) {
                    loc.setX(Math.max(boundingBox.getMinX() , Math.min(loc.getX(), boundingBox.getMaxX())));
                    loc.setY(Math.max(boundingBox.getMinY() , Math.min(loc.getY(), boundingBox.getMaxY())));
                    loc.setZ(Math.max(boundingBox.getMinZ() , Math.min(loc.getZ(), boundingBox.getMaxZ())));
                    snowball.remove();
                    Vector vector = new Vector(boundingBox.getCenterX(), 1, boundingBox.getCenterZ()).subtract(loc.toVector());
                    vector.normalize().multiply(0.5);
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.SNOWBALL)).setVelocity(vector);;
                    this.cancel();
                };
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1L);
    }
    
}
