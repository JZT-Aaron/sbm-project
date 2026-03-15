package de.dachente.sbm.listeners;

import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Team;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;


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
            int amount = 1;

            if(hitPlayer.getHealthScale() <= 2) amount = 2;
            
            Game.dropBonusSnowball(team, amount);
           
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5, 1);
            return;
        }

        snowball.getWorld().dropItem(snowball.getLocation(), snowball.getItem());
    }

    
}
