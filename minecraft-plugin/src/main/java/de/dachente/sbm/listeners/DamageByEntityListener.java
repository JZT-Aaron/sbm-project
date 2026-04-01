package de.dachente.sbm.listeners;

import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Team;

import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageByEntityListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;
        if(!(Game.isRunning() && Game.getLivingPlayers().containsKey(player.getUniqueId().toString())) || Game.state().equals(GameState.PAUSED)) {
            event.setCancelled(true);
            return;
        }

        // While Game is not Running no Damange can be Dealt  
        if(!Game.state().equals(GameState.RUNNING_MATCH)) {
            return;
        }

        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());
        Player damager = null;

        // Just hit by Snowball
        if(event.getDamager() instanceof Snowball snowball) if(snowball.getShooter() instanceof Player shooter) damager = shooter;
        else return;

        if(damager == null) throw new IllegalArgumentException("A Snowball hit without shooter was detected.");
        
        // No Friendly Fire
        if(TeamManager.getTeamPlayers(team).contains(damager.getUniqueId().toString())) {
            event.setCancelled(true);
            return;
        }

        
        if(Game.isRunning() && (TeamManager.getTeamsPlayer().get(damager.getUniqueId().toString()) != team)) {
            // Ask if the hit is fatal.
            boolean isKilled = (player.getHealthScale() <= 2);
            if(isKilled) Game.deadMode(player);
            else {
                player.setHealthScale(player.getHealthScale()-2);
                Game.respawnPlayer(player);
            } 
            
            Info.sendLangInfo("event." + (isKilled ? "player-died" : "player-hit"), "%target%", team.getChatColor() + player.getName(), "%damager%", TeamManager.getOppositeTeam(team).getChatColor() + damager.getName());
            
            Game.updateTeamHearts();
        }
    }

}
