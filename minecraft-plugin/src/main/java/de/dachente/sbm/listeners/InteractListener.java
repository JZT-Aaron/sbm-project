package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Server;

import org.bukkit.GameMode;
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
        if(event.getItem() == null) return;
        if(!((player.hasPermission(config.getString("permission.sbm.allow.item-move")) && player.getGameMode().equals(GameMode.CREATIVE)) || (event.getItem().getType().equals(Material.SNOWBALL))
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
            if(!Game.state().equals(GameState.OPEN)) {
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

        if(id.equalsIgnoreCase("open-change-language")) {
            LanguageManager.openLanguageMenu(player);
            return;
        }

        if(id.equalsIgnoreCase("join-lobby")) {
            Main.joinServer(Server.LOBBY, player);
            return;
        }

        if(id.equalsIgnoreCase("join-game-server")) {
            Main.joinServer(Server.EVENT_SERVER, player);
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
