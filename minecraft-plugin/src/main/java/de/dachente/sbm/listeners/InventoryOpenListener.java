package de.dachente.sbm.listeners;

import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

import de.dachente.sbm.utils.Game;

public class InventoryOpenListener implements Listener {

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if(event.getInventory().getHolder() instanceof Dropper && Game.getLivingPlayers().containsKey(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            return;
        } 
    }
    
}
