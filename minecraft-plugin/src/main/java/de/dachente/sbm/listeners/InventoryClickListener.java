package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import de.dachente.sbm.utils.enums.Language;
import net.kyori.adventure.text.Component;

import static de.dachente.sbm.managers.LanguageManager.getText;

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


        if(ItemBuilder.isUnmovable(item) && !(player.hasPermission(Main.getPlugin().getConfig().getString("permission.sbm.allow.item-move")) && player.getGameMode().equals(GameMode.CREATIVE)) ) {
            event.setCancelled(true);
        }

        String id = ItemBuilder.getTagData(item);
        if(id == null) return;

        if(id.contains("use-camera")) {
            String cameraId = id.replace("use-", "");
            for(ArmorStand armorStand : Game.cameraPoints) {
                if(armorStand.customName() == null) continue;
                if(!Main.toPlain(armorStand.customName()).equalsIgnoreCase(cameraId)) continue;
                player.setGameMode(GameMode.SPECTATOR);
                player.setSpectatorTarget(armorStand);
                player.sendActionBar(Component.text("§7§o" +  getText("info.sneak-to-leave", player.getUniqueId())));
            }
            player.closeInventory();
            return;
        }

        if(id.contains("select-lang-")) {
            Language lang = Language.valueOf(id.replace("select-lang-", ""));
            if(LanguageManager.hasLanguage(player.getUniqueId(), lang)) return;
            LanguageManager.setLanguage(player, lang);
            LanguageManager.openLanguageMenu(player);
        }
    }
}
