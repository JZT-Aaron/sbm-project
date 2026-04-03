package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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

        // Proxy Player System - Quit
        if(Game.getLivingPlayers().containsKey(uuid)) {
            Game.removeFromLivingPlayers(uuid);
            Game.addLeftPlayer(player);
            Game.dropInvSnowballs(player);
            Team team = TeamManager.getTeam(player);
            Info.sendLangImportantInfo("left-player.add", "%team%", team.getChatColor() + team.getName());
        }

        LanguageManager.removeOnline(player.getUniqueId());
        MutliLangSignManager.customSigns.remove(player.getUniqueId());
    }
}
