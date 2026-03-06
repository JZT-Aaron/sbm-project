package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import net.kyori.adventure.text.Component;

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
        Player player = event.getPlayer();
        event.joinMessage(Component.empty());
        Info.sendInfo("§a➕ §7§oDer Spieler §7" + Main.toPlain(player.displayName()) + " §oist jetzt auf dem Server!", "§cServer");

        if(!player.hasPermission(config.getString("permission.sbm.allow.behold-gamemode"))) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName())) {
            if(!Game.isOpen()) Main.joinServer(Server.LOBBY, player);
            if(!Game.getLivingPlayers().contains(player.getUniqueId().toString())) {
                Game.setGameServerHotbar(player);
                if(TeamManager.isInTeam(player)) StatusManger.setPlayerStatus(Status.DEAD, player);
                else StatusManger.setPlayerStatus(Status.WATCHING, player);
            } 
            if(Game.isRoundGoing) Game.bossBar.addPlayer(player);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.lobby.getName())) {
            Main.joinServer(Server.LOBBY, player, true);
        }

        StatusManger.updatePlayerStatus(player);

        if(TeamManager.getTeamsPlayer().containsKey(uuid)) {
            Team team = TeamManager.getTeamsPlayer().get(uuid);
            if(!Game.leftTeamPlayers.contains(uuid) && Game.getLivingPlayers().containsKey(uuid)) {
                Info.sendInfo("Du musst diese Runde zuschauen!", player);
                player.teleport(Main.arena.getSpawnLocation());
                Game.removeFromLivingPlayers(null);
            }
            TeamManager.getTeamPlayers(team).add(uuid);
        }
    }
}
