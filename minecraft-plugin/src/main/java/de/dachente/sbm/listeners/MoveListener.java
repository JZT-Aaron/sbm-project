package de.dachente.sbm.listeners;

import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.GameState;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(!Game.state().equals(GameState.RUNNING_REMATCH) || !TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            return;
        }

        //Team team = Game.getTeamsPlayer().get(player.getUniqueId().toString());

        if(player.getLocation().getY() < -45) {
            Game.deadMode(player);
        }
    }
}
