package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Repeat;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Gate;
import de.dachente.sbm.utils.enums.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class MoveListener implements Listener {
        

    Map<UUID, Gate> gatePlayers = new HashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(Game.isSnowing) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if(from.distanceSquared(to) < 0.046) Repeat.movingPlayers.remove(player.getUniqueId());
            else if(!Repeat.movingPlayers.contains(player.getUniqueId())) Repeat.movingPlayers.add(player.getUniqueId());
        }
        
        if(Main.resendLobbySigns.contains(player)) {
            Main.resendLobbySigns.remove(player);
            StartClock.updateSigns(player, true);
        }

        UUID uuid = player.getUniqueId();

        // Detect if Barrier at Gates need to be removed.
        if(TeamManager.getTeamsPlayer().containsKey(uuid.toString())) {
            Team team = TeamManager.getTeam(player);
            if(!gatePlayers.containsKey(uuid)) {
                Gate gate = GateManager.getGateForBarrierBox(player.getLocation(), team);
                if(gate != null) {
                    if(!gatePlayers.containsValue(gate)) GateManager.setBarriers(gate, true);
                    gatePlayers.put(uuid, gate);
                }
            } else {
                Gate gate = gatePlayers.get(uuid);
                if(GateManager.containsBarrierBoxLoc(gate, player.getLocation())) return; 
                gatePlayers.remove(uuid);
                if(!gatePlayers.containsValue(gate)) {
                    GateManager.setBarriers(gate, false);
                }
            }
        }

        // Prevent Access to the Circle to the Doom Bell
        if(Game.state().equals(GameState.RUNNING_MATCH)) {
            for(int i = 0; i < 2; i++) {
                List<Location> zonePoses = Main.parseList(Main.getPlugin().getConfig().getString("target.square"));
                BoundingBox zone = BoundingBox.of(zonePoses.get(2*i), zonePoses.get((i*2)+1));

                List<Location> poses = Main.parseList(Main.getPlugin().getConfig().getString("target.circle"));
                Location von = event.getFrom();
                Location zu = event.getTo();

                if(von.getX() == zu.getX() && von.getZ() == von.getZ() || von.getY() > 7) return;

                Location mitte = poses.get(i);

                double dx = zu.getBlockX() - mitte.getBlockX();
                double dz = zu.getBlockZ() - mitte.getBlockZ();

                double distanzQuadrat = (dx * dx) + (dz * dz);

                if (distanzQuadrat <= 20 || zone.contains(player.getLocation().toVector())) {
                    double dxVon = von.getBlockX() - mitte.getBlockX();
                    double dzVon = von.getBlockZ() - mitte.getBlockZ();
                    double distanzVonQuadrat = (dxVon * dxVon) + (dzVon * dzVon);

                    if (distanzQuadrat < 9 && distanzVonQuadrat >= 9) {
                        von.setYaw(zu.getYaw());
                        von.setPitch(zu.getPitch());
                        von.setY(zu.getY());
                        event.setTo(von);
                    }
                    Vector vector = new Vector(dx, 0, dz).normalize().multiply(0.5);
                    vector.setY(0);
                    Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                        player.setVelocity(vector);
                    });
                    break;
                }
            }
            
        }
     

        // Detection if Rematch fall from Platform
        if((Game.state().equals(GameState.RUNNING_REMATCH) || Game.state().equals(GameState.PAUSED)) && Game.getLivingPlayers().containsKey(player.getUniqueId().toString()) && player.getLocation().getY() < -30) {
            if(Game.state().equals(GameState.PAUSED))  {
                player.teleport(Game.getRematchSpawnLocation(player));
                return;
            }
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
