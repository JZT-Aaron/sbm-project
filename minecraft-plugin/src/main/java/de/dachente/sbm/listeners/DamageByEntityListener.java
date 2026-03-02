package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageByEntityListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if(player.getWorld().equals(Main.lobby) || !TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString()) || !Game.isRoundGoing
                || (!Game.livingPlayersTeamBlue.contains(player.getUniqueId().toString()) && !Game.livingPlayersTeamRed.contains(player.getUniqueId().toString()))) {
            event.setCancelled(true);
            return;
        }

        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());
        Player damager = null;

        if(event.getDamager() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getDamager();
            if(snowball.getShooter() instanceof Player) {
                damager = (Player) snowball.getShooter();
            }
        }

        if(event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        }

        if(damager == null) return;
        if(!TeamManager.getTeamsPlayer().containsKey(damager.getUniqueId().toString())) {
            event.setCancelled(true);
            return;
        }

        if(Game.isRoundGoing && (TeamManager.getTeamsPlayer().get(damager.getUniqueId().toString()) != team)) {
            Info.sendInfo("Der Spieler " + team.getChatColor() + player.getName() + " §7§owurde von " + TeamManager.getOppositeTeam(team).getChatColor() + damager.getName() +" §7§ogetroffen.");
            if(player.getHealthScale() <= 2) {
                Info.sendInfo("Der Spieler " + team.getChatColor() + player.getName() + " §7§oist durch " + TeamManager.getOppositeTeam(team).getChatColor() + damager.getName() +" §7§ogestorben.");
                Game.deadMode(player);
            } else
                player.setHealthScale(player.getHealthScale()-2);
            Game.updateTeamHearts();
            event.setCancelled(false);
        }
    }

}
