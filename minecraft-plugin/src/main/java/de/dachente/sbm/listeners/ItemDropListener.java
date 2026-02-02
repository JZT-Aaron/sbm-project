package de.dachente.sbm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import de.dachente.sbm.utils.ItemBuilder;

public class ItemDropListener implements Listener {

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if(!ItemBuilder.hasPersistedDataContainer(item)) return;

        if(ItemBuilder.isUnmovable(item)) {
            event.setCancelled(true);
        }
    }
}
