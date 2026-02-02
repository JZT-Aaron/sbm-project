package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;
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
        Game.sendInfo("§a✓ §7§oDer Spieler §7" + Main.toPlain(player.displayName()) + " §oist jetzt auf dem Server!", "§cServer");

        if(!player.hasPermission(config.getString("permission.sbm.allow.behold-gamemode"))) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName()) && !Game.isOpen()) {
            player.teleport(Main.lobby.getSpawnLocation());
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName()) && !Game.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            Game.setGameServerHotbar(player);
        }

        if(player.getWorld().getPlayers().equals(Main.arena.getName()) && Game.isStarted) {
            Game.bossBar.addPlayer(player);
        }

        if(player.getWorld().getName().equalsIgnoreCase(Main.lobby.getName()) && !Game.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            Game.setLobbyHotbar(player);
        }

        if(Game.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            Team team = Game.getTeamsPlayer().get(player.getUniqueId().toString());
            if(!Game.leftTeamPlayers.contains(player.getUniqueId().toString()) &&
                    (Game.livingPlayersTeamRed.contains(player.getUniqueId().toString()) || Game.livingPlayersTeamBlue.contains(player.getUniqueId().toString()))) {
                Game.sendInfo("Du musst diese Runde zuschauen!", player);
                player.teleport(Main.arena.getSpawnLocation());
                Game.getLivingPlayers(team).remove(player.getUniqueId().toString());
            }
            player.playerListName(Component.text(team.getChatColor() + player.displayName()));
            Game.getTeamPlayers(team).add(player.getUniqueId().toString());
        }
    }
}
