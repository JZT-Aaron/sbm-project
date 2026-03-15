package de.dachente.sbm.listeners;

import de.dachente.sbm.utils.Game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if(Game.getLivingPlayers().containsKey(player.getUniqueId().toString()) && Game.isRunning()) {
            event.setDamage(0);
            return;
        }
        event.setCancelled(true);
        
    }
}
