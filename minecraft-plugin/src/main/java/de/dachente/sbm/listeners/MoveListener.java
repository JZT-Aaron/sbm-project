package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.enums.GameState;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(Main.resendLobbySigns.contains(player)) {
            Main.resendLobbySigns.remove(player);
            StartClock.updateSigns(player, true);
        }
        if(Game.state().equals(GameState.RUNNING_REMATCH) && TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString()) && player.getLocation().getY() < -30) {
            Info.sendLangInfo("rematch.player-hit", "%player%", TeamManager.getTeam(player).getChatColor() + player.getName());
            if(player.getHealthScale() > 2) {
                player.setHealthScale(player.getHealthScale()-2);
                Game.updateTeamHearts();
                player.teleport(Game.getRematchSpawnLocation(player));
                return;
            } 
            Game.deadMode(player);
        }
    }
}
