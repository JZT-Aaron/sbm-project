package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.DemoManger;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.PlayerStats;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.bukkit.Bukkit;
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
            Main.getPlugin().getLogger().info("Teams before Living Remvoe: " + TeamManager.getTeamsPlayer());
            Game.removeFromLivingPlayers(uuid);
            Main.getPlugin().getLogger().info("Teams after Living Remvoe: " + TeamManager.getTeamsPlayer());
            if(Game.isRunning() || Game.state().equals(GameState.PAUSED)) {
                boolean success = Game.addLeftPlayer(player);
                Game.dropInvSnowballs(player);
                Team team = TeamManager.getTeam(player);
                if(success) Info.sendLangImportantInfo("left-player.add", "%team%", team.getChatColor() + team.getName());
                else Info.sendLangImportantInfo("left-player.none", "%team%", team.getChatColor() + team.getName());
            }
        }

        LanguageManager.removeOnline(player.getUniqueId());
        MutliLangSignManager.customSigns.remove(player.getUniqueId());
        PlayerStats.setOffline(player.getUniqueId());

        if(Main.isDemo && Bukkit.getOnlinePlayers().size() <= 1) {
            Main.getPlugin().getLogger().info("Detected Empty Server: Closing in 15 Minutes");
            DemoManger.setCloseTimestamp(Instant.now().plus(15, ChronoUnit.MINUTES));
        } 
    }
}
