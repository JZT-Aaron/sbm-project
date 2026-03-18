package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class QuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        event.quitMessage(Component.empty());
        Info.sendLangInfo("event.player-left", "%player%", player.getName());
        
        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName()) && BossBarManager.containsPlayer(player)) {
            BossBarManager.removePlayer(player);
        }

        if(Game.getLivingPlayers().containsKey(uuid)) {
            Game.removeFromLivingPlayers(uuid);
            Game.addLeftPlayer(player);
            if(player.getInventory().getContents().length > 0)
                for(ItemStack item : player.getInventory().getContents()) {
                    if(item == null || !item.getType().equals(Material.SNOWBALL)) continue;
                    player.getInventory().remove(item);
                    Main.arena.dropItemNaturally(player.getLocation(), item);
                }
            Team team = TeamManager.getTeam(player);
            Info.sendLangImportantInfo("left-player.add", "%team%", team.getChatColor() + team.getName());
        }

        LanguageManager.removeOnline(player.getUniqueId());
        MutliLangSignManager.customSigns.remove(player.getUniqueId());
    }
}
