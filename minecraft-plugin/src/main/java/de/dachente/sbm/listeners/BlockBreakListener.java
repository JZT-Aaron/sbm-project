package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    FileConfiguration config = Main.getPlugin().getConfig();

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(!player.hasPermission(config.getString("permission.sbm.allow.break-blocks"))) {
           event.setCancelled(true);
        }
    }
}
