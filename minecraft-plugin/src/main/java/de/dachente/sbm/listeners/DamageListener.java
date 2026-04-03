package de.dachente.sbm.listeners;

import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.GameState;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        //Damage Animation If Player is right now Gaming else completly canceled.
        if(Game.getLivingPlayers().containsKey(player.getUniqueId().toString()) && Game.isRunning() && !Game.state().equals(GameState.PAUSED)) {
            if(!(event.getDamageSource().getCausingEntity() instanceof Player damager)) return;
            if(TeamManager.getTeam(player).equals(TeamManager.getTeam(damager))) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(0);
            return;
        }
        event.setCancelled(true);
        
    }
}
