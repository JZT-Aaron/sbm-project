package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import de.dachente.sbm.utils.enums.Language;
import net.kyori.adventure.text.Component;

import static de.dachente.sbm.managers.LanguageManager.getText;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

        if(!ItemBuilder.hasTagData(item)) return;

        String id = ItemBuilder.getTagData(item);
        if(id == null) return;

        //Menu for Camera

        if(id.contains("use-camera")) {
            for(Player all : Bukkit.getOnlinePlayers()) all.hidePlayer(Main.getPlugin(), player);
            
            String cameraId = id.replace("use-", "");
            Location loc = Game.cameraPoints.get(cameraId);
            player.teleport(loc);
            ArmorStand anchor = player.getWorld().spawn(player.getLocation(), ArmorStand.class, (as) -> {
                as.setInvisible(true);
                as.setMarker(false); 
                as.setInvulnerable(true);
                as.setSmall(true);
                as.setBasePlate(false);
                as.setGravity(false);
                as.addScoreboardTag("camera_anchor");
            });

            anchor.addPassenger(player);
            player.getInventory().clear();
            player.getInventory().setItem(4, new ItemBuilder(Material.SPYGLASS).setUnmovable().build());
            String idArrmorstand = player.getUniqueId().toString();
            anchor.addScoreboardTag(idArrmorstand);
            SpectatorManager.addSpectatorPlayers(player, idArrmorstand);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> player.sendActionBar(Component.text("§7§o" + getText("info.sneak-to-leave", player.getUniqueId()))), 2L);
            return;
        }

        //Menu for Lang

        if(id.contains("select-lang-")) {
            Language lang = Language.valueOf(id.replace("select-lang-", ""));
            if(LanguageManager.hasLanguage(player.getUniqueId(), lang)) return;
            LanguageManager.setLanguage(player, lang);
            LanguageManager.openLanguageMenu(player);
        }
    }
}
