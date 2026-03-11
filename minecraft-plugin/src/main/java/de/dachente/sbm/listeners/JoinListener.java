package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.BossBarManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.StatusManger;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.utils.enums.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    FileConfiguration config = Main.getPlugin().getConfig();

    //TODO: Left Player hearts get distributed and then redirected.

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        event.joinMessage(Component.empty());
        Info.sendLangInfo("event.player-join", "%player%", player.getName());

        if(!player.hasPermission(config.getString("permission.sbm.allow.behold-gamemode"))) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName())) {
            if(!Game.isOpen()) Main.joinServer(Server.LOBBY, player);
            if(!Game.getLivingPlayers().containsKey(uuid)) {
                Game.setGameServerHotbar(player);
                if(TeamManager.isInTeam(player)) StatusManger.setPlayerStatus(Status.DEAD, player);
                else StatusManger.setPlayerStatus(Status.WATCHING, player);
            } 
            if(Game.isRunning()) BossBarManager.addPlayer(player);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.lobby.getName())) {
            Main.joinServer(Server.LOBBY, player, false);
        }

        StatusManger.updatePlayerStatus(player);

        if(TeamManager.getTeamsPlayer().containsKey(uuid)) {
            Team team = TeamManager.getTeamsPlayer().get(uuid);
            if(!Game.leftTeamPlayers.contains(uuid) && Game.getLivingPlayers().containsKey(uuid)) {
                Info.sendLangInfo("only-watch-round", player);
                player.teleport(Main.arena.getSpawnLocation());
            }
            TeamManager.getTeamPlayers(team).add(uuid);
        }      
    }
}
