package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(Component.empty());
        Game.sendInfo("§c✗ §7§oDer Spieler §7" + Main.toPlain(player.displayName()) + " §oist jetzt nicht mehr auf dem Server!", "§cServer");

        if(player.getWorld().getName().equalsIgnoreCase(Main.arena.getName()) && Game.bossBar.getPlayers().contains(player)) {
            Game.bossBar.removePlayer(player);
        }

        if(Game.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            Game.leftTeamPlayers.add(player.getUniqueId().toString());
            Team team = Game.getTeamsPlayer().get(player.getUniqueId().toString());
            Game.getLivingPlayers(team).remove(player.getUniqueId().toString());
        }


    }
}
