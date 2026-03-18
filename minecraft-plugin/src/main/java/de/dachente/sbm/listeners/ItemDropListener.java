package de.dachente.sbm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import de.dachente.sbm.utils.enums.GameState;

public class ItemDropListener implements Listener {

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if(Game.getLivingPlayers().containsKey(event.getPlayer().getUniqueId().toString()) && Game.state().equals(GameState.PAUSED)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();

        if(!ItemBuilder.hasPersistedDataContainer(item)) return;

        if(ItemBuilder.isUnmovable(item) || ItemBuilder.isUnDroppable(item)) {
            event.setCancelled(true);
        }
    }
}
