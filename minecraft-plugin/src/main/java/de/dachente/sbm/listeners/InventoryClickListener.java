package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(event.getCurrentItem() == null) return;

        ItemStack item = event.getCurrentItem();

        if(!ItemBuilder.hasPersistedDataContainer(item)) return;


        if(ItemBuilder.isUnmovable(item)) {
            event.setCancelled(true);
        }

        String id = ItemBuilder.getTagData(item);

        if(id.contains("use-camera")) {
            String cameraId = id.replace("use-", "");
            for(ArmorStand armorStand : Game.cameraPoints) {
                if(armorStand.customName() == null) continue;
                if(!Main.toPlain(armorStand.customName()).equalsIgnoreCase(cameraId)) continue;
                player.setGameMode(GameMode.SPECTATOR);
                player.setSpectatorTarget(armorStand);
                player.sendActionBar(Component.text("§7§oSneke um zu verlassen."));
            }
            player.closeInventory();
            return;
        }
    }
}
