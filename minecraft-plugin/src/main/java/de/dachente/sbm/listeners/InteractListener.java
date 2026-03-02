package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.net.MalformedURLException;

public class InteractListener implements Listener {

    FileConfiguration config = Main.getPlugin().getConfig();

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event) throws MalformedURLException {
        Player player = event.getPlayer();
        if(!(player.hasPermission(config.getString("permission.sbm.allow.interact")) || (event.getItem().getType().equals(Material.SNOWBALL))
                && TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString()))) {
            event.setCancelled(true);
        }
        
        if(event.getItem() == null) return;
        if(!event.getItem().hasItemMeta()) return;
        if(event.getItem().getItemMeta().getPersistentDataContainer().isEmpty()) return;


        String id = ItemBuilder.getTagData(event.getItem());

        if(ItemBuilder.isUnmovable(event.getItem())) {
            event.setCancelled(true);
        }

        if(id.equalsIgnoreCase("join-team")) {
            event.setCancelled(true);
            if(!Game.isJoiningOpen) {
                Info.sendInfo("Du kannst nicht mehr Teilnehmen!", player);
                return;
            }
            TeamManager.addPlayerTeam(player.getUniqueId().toString());
            player.getInventory().setHeldItemSlot(3);
            return;
        }

        if(id.equalsIgnoreCase("open-camera-views")) {
            Game.openCameraViews(player);
            return;
        }

        if(id.equalsIgnoreCase("join-lobby")) {
            player.teleport(Main.lobby.getSpawnLocation());
            Game.setLobbyHotbar(player);
            return;
        }

        if(id.equalsIgnoreCase("join-game-server")) {
            player.teleport(Main.arena.getSpawnLocation());
            Game.setGameServerHotbar(player);
            return;
        }

        if(id.equalsIgnoreCase("join-parkour")) {
            player.teleport(new Location(Main.lobby, -4.5, 15, 4.5, 180, 0));
            return;
        }

        if(id.equalsIgnoreCase("leave-team")) {
            TeamManager.removePlayerTeam(player.getUniqueId().toString());
            return;
        }
    }
}
