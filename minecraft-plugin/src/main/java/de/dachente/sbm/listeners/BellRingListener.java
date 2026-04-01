package de.dachente.sbm.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.scheduler.BukkitRunnable;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Team;

public class BellRingListener implements Listener {

    @EventHandler
    public void onRing(BellRingEvent event) {
        Block bell = event.getBlock();
        List<Location> bells = Main.parseList(Main.getPlugin().getConfig().getString("target.pos"));
        if(!bells.contains(bell.getLocation())) return;
        // Pause game to prevent changes in cames outcome.
        Game.pause(false);

        // Get Team from Bell Location 
        int teamID = bells.indexOf(bell.getLocation());
        Team team = teamID == 0 ? Team.BLUE : Team.RED;
        
        // Ring Bell 3 Times
        new BukkitRunnable() {
            int count = 3;
            public void run() {
                for(Player all : Main.arena.getPlayers()) all.playSound(all.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                count--;
                if(count <= 0) this.cancel();
            }
        }.runTaskTimer(Main.getPlugin(), 0, 20);
        Info.showLangTitle("doom-bell", "%team%", team.getChatColor() + " @team." + team.getId());

        // After Bell is Rang 3 Time Doomed Team is Eliminated.
        new BukkitRunnable() {
            public void run() {
                for(String teamPlayerUUid : Game.getLivingPlayers(team)) {
                    Player teamPlayer = Bukkit.getPlayer(UUID.fromString(teamPlayerUUid));
                    if(teamPlayer == null) {
                        Game.removeFromLivingPlayers(teamPlayerUUid);
                        return;
                    }
                    Game.deadMode(teamPlayer);
                }
            }
        }.runTaskLater(Main.getPlugin(), 80);
    }
    
}
