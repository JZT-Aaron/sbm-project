package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.DemoManger;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Game.HandoverContext;
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

        // Player Join on Game Server
        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName())) {
            if(!Game.isOpen()) Main.joinServer(Server.LOBBY, player);
            Game.setViewer(player);
            if(Game.hasStarted()) BossBarManager.addPlayer(player);
        }   

        StatusManger.updatePlayerStatus(player);


        //Proxy Player System Join
        if(TeamManager.getTeamsPlayer().containsKey(uuid) && Game.getLeftPlayers().contains(uuid)) {
            Main.getPlugin().getLogger().info(player.getName() + " detected as a Left Player, handover Context is Provided.");
            Game.HandoverContext handoverContext = Game.getLeftPlayerHandoverContext(player.getUniqueId());
            Game.addToLivingPlayers(uuid);
            boolean proxyPlayerExisting = handoverContext.proxyUuid() != null;
            

            UUID proxyUuid = proxyPlayerExisting ? UUID.fromString(handoverContext.proxyUuid()) : null;
            Player proxyPlayer = proxyPlayerExisting ? Bukkit.getPlayer(proxyUuid) : null;
            
            boolean proxyLeft = proxyPlayerExisting ? Game.getLeftPlayers().contains(handoverContext.proxyUuid()) : false;
 
            Integer leftProxyHearts;
            if(!proxyPlayerExisting) leftProxyHearts = handoverContext.playerHearts();
            else {
                Integer currentHearts = proxyLeft || proxyPlayer == null ? Game.getLeftPlayerHandoverContext(proxyUuid).playerHearts() : ((int) proxyPlayer.getHealthScale());
                leftProxyHearts = currentHearts - handoverContext.proxyHearts();
            } 

            Game.removeLeftPlayer(player);
            if(leftProxyHearts <= 0) {
                Info.sendLangInfo("only-watch-round", player);
                Game.setViewer(proxyPlayer);
                StatusManger.setPlayerStatus(Status.DEAD, proxyPlayer);
                return;
            } else {
                player.setHealthScale(leftProxyHearts);
                if(proxyLeft) {
                    HandoverContext proxyHandoverContext = Game.getLeftPlayerHandoverContext(proxyUuid);
                    Game.setHandoverContext(proxyUuid, new HandoverContext(proxyHandoverContext.proxyUuid(), handoverContext.proxyHearts(), proxyHandoverContext.proxyHearts()));
                } else if(proxyPlayerExisting) proxyPlayer.setHealthScale(handoverContext.proxyHearts());

                Game.addToLivingPlayers(uuid);
                Game.respawnPlayer(player);
                StatusManger.setPlayerStatus(Status.PLAYING, player);
            }
        }           
    }
}
