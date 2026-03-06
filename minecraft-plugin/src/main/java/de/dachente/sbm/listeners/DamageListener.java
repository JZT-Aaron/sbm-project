package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.TeamManager;
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
        if(player.getWorld().equals(Main.lobby) || !TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString()) ||   
            !Game.isRunning() || Game.getLivingPlayers().containsKey(player.getUniqueId().toString())) {
            event.setCancelled(true);
        }
    }
}
