package de.dachente.sbm.listeners;

import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Team;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;


public class SnowballHitListener implements Listener {

    @EventHandler
    public void onSnowballThrow(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Snowball)) return;
        if(!(event.getEntity().getShooter() instanceof Player)) return;

        Snowball snowball = (Snowball) event.getEntity();
        Player player = (Player) snowball.getShooter();

        if(!TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) return;

        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());

        //Bonus Snowball or Normal drop weather friendly or enemy.
        if(event.getHitEntity() != null && (event.getHitEntity() instanceof Player)) {
            Player hitPlayer = (Player) event.getHitEntity();
            Team hitTeam = TeamManager.getTeamsPlayer().get(hitPlayer.getUniqueId().toString());
            if(hitTeam == team) {
                snowball.getWorld().dropItem(snowball.getLocation(), snowball.getItem());
                return;
            }
            int amount = 1;

            if(hitPlayer.getHealthScale() <= 2) amount = 2;
            
            Game.dropBonusSnowball(team, amount);
           
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5, 1);
            return;
        }

        //Normal Block hit with Reflextion of Snowball

        Location dropLocation = snowball.getLocation();

        Block block = event.getHitBlock();
        BlockFace blockFace = event.getHitBlockFace();

        if(block != null && blockFace != null) {
            dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

            Vector offest = blockFace.getDirection().multiply(1.1);
            dropLocation.add(offest);
        }

        snowball.getWorld().dropItemNaturally(dropLocation, snowball.getItem());

        List<Item> snowballs = dropLocation.getNearbyEntities(0.5, 0.5, 0.5).stream().filter((entity) -> {
            if(entity instanceof Item item) return (item.getItemStack().getType() == Material.SNOWBALL);
            return false; 
        }).map((enity) -> (Item) enity).toList();

        Vector vector =  getReflextionVector(blockFace, snowball.getVelocity());
        
        for(Item cSnowball : snowballs) {
            cSnowball.setVelocity(vector);
        }
    }

    public static Vector getReflextionVector(BlockFace blockFace, Vector vector) {
        if(blockFace != null) {
            Vector blockFaceVector = blockFace.getDirection();

            double dotProduct = vector.dot(blockFaceVector);
            vector = vector.subtract(blockFaceVector.multiply(2* dotProduct));
        }
        vector.normalize();
        vector.multiply(0.5);
        return vector;
    }

    
}
