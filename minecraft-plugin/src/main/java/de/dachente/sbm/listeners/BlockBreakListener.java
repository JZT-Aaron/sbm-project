package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.ItemBuilder;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockBreakListener implements Listener {

    FileConfiguration config = Main.getPlugin().getConfig();

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(!(player.hasPermission(config.getString("permission.sbm.allow.break-blocks")) && player.getGameMode().equals(GameMode.CREATIVE))) {
           event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if(!(player.hasPermission(config.getString("permission.sbm.allow.break-blocks")) && player.getGameMode().equals(GameMode.CREATIVE) && !ItemBuilder.isUnmovable(player.getInventory().getItemInMainHand()))) {
           event.setCancelled(true);
        }
    }
}
