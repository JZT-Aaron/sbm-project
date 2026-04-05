package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import net.kyori.adventure.text.Component;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    FileConfiguration config = Main.getPlugin().getConfig();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(Main.isDemo && DemoManger.timestamp != null) {
            Main.getPlugin().getLogger().info("Detectet Join: Closing is now undefined.");
            DemoManger.setCloseTimestamp(null); 
        } 
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        event.joinMessage(Component.empty());
        Info.sendLangInfo("event.player-join", "%player%", player.getName());

        if(!player.hasPermission(config.getString("permission.sbm.allow.behold-gamemode"))) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.lobby.getName())) {
            Main.joinServer(Server.LOBBY, player, false);
            return;
        }

        StatusManger.updatePlayerStatus(player);


        //Proxy Player System Join
        if(TeamManager.getTeamsPlayer().containsKey(uuid) && Game.getLeftPlayers().contains(uuid)) {
            Game.HandoverContext handoverContext = Game.getLeftPlayerHandoverContext(player.getUniqueId());
            if(handoverContext.proxyUuid() == null) {
                Game.addToLivingPlayers(uuid);
                return;
            }
            Player proxyPlayer = Bukkit.getPlayer(UUID.fromString(handoverContext.proxyUuid()));
            Integer leftProxyHearts = ((int) proxyPlayer.getHealthScale()) - handoverContext.proxyHearts();
            Info.sendInfo("Hearts: " + leftProxyHearts);
            Game.removeLeftPlayer(player);
            if(leftProxyHearts <= 0) {
                Info.sendLangInfo("only-watch-round", player);
                Game.setViewer(proxyPlayer);
                StatusManger.setPlayerStatus(Status.DEAD, proxyPlayer);
                return;
            } else {
                player.setHealthScale(leftProxyHearts);
                proxyPlayer.setHealthScale(handoverContext.proxyHearts());
                Game.addToLivingPlayers(uuid);
                Game.respawnPlayer(player);
            }
            return;
        }      

        // Player Join in Worlds
        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName())) {
            if(!Game.isOpen()) Main.joinServer(Server.LOBBY, player);
            Game.setViewer(player);
            if(Game.isRunning()) BossBarManager.addPlayer(player);
        }        
    }
}
