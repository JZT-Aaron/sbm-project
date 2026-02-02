package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerToggleSnakeListener implements Listener {

    @EventHandler
    public void onSnake(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if(player.getSpectatorTarget() == null) return;
        if(!(player.getSpectatorTarget() instanceof ArmorStand)) return;
        ArmorStand armorStand = (ArmorStand) player.getSpectatorTarget();
        if(!Game.cameraPoints.contains(armorStand)) return;
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(Main.arena.getSpawnLocation());
    }
}
