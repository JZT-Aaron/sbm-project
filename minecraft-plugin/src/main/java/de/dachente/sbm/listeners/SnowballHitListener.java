package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Team;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

public class SnowballHitListener implements Listener {

    @EventHandler
    public void onSnowballThrow(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Snowball)) return;
        if(!(event.getEntity().getShooter() instanceof Player)) return;

        Snowball snowball = (Snowball) event.getEntity();
        Player player = (Player) snowball.getShooter();

        if(!TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) return;

        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());

        if(event.getHitEntity() != null && (event.getHitEntity() instanceof Player)) {
            Player hitPlayer = (Player) event.getHitEntity();
            Team hitTeam = TeamManager.getTeamsPlayer().get(hitPlayer.getUniqueId().toString());
            if(hitTeam == team) {
                snowball.getWorld().dropItem(snowball.getLocation(), snowball.getItem());
                return;
            }
            ItemStack snowballItem = snowball.getItem();
            if(hitPlayer.getHealthScale() <= 2) {
                snowballItem.setAmount(3);
            }
            if(team == Team.BLUE) {
                player.getWorld().dropItem(new Location(Main.arena, 0.5, 3, 7.5), snowballItem);
            }
            if(team == Team.RED) {
                player.getWorld().dropItem(new Location(Main.arena, 0.5,3,-6.5), snowballItem);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5, 1);
            return;
        }

        snowball.getWorld().dropItem(snowball.getLocation(), snowball.getItem());

    }
}
