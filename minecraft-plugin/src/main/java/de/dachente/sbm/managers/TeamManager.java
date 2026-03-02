package de.dachente.sbm.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;

public class TeamManager {

    private static Map<String, Team> teamsPlayerUUIDs = new HashMap<>();
    
    public static void addPlayerTeam(String uuid) {
        Team team = null;
        if(getTeamPlayers(Team.RED).size() < getTeamPlayers(Team.BLUE).size()) {
            team = Team.RED;
        }
        if(getTeamPlayers(Team.BLUE).size() < getTeamPlayers(Team.RED).size()) {
            team = Team.BLUE;
        }
        if(getTeamPlayers(Team.RED).size() == getTeamPlayers(Team.BLUE).size()) {
            int i = new Random().nextInt(2);
            if(i == 0) {
                team = Team.BLUE;
            }
            if(i == 1) {
                team = Team.RED;
            }
        }
        addPlayerTeam(uuid, team);
    }

    public static void addPlayerTeam(String uuid, Team team) {
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        player.getInventory().clear();
        //Location teamSpawnLocation = null;

        teamsPlayerUUIDs.put(uuid, team);

        player.teleport(team.getTeamSpawnLocation());

        Game.loadLobbyInv(player);
        player.updateInventory();

        Game.setTeamChestPlate(team, player);
        Info.sendInfo("§oDu bist jetzt in " + team.getChatColor() + team.getName() + "§7.", "Info", Bukkit.getPlayer(UUID.fromString(uuid)));
    }

    public static void removePlayerTeam(String uuid) {
        Team team = teamsPlayerUUIDs.get(uuid);
        if(team == null) return;

        Player player = Bukkit.getPlayer(UUID.fromString(uuid));

        teamsPlayerUUIDs.remove(uuid);

        Game.setViewer(player);    
        Info.sendInfo("§oDu bist jetzt nicht mehr " + team.getChatColor() + team.getName() + "§7.", Bukkit.getPlayer(UUID.fromString(uuid)));
    }


    public static void clearTeam(Team team) {
        for(String uuid : getTeamPlayers(team)) {
            removePlayerTeam(uuid);
        }
    }

    public static Map<String, Team> getTeamsPlayer() {
        return teamsPlayerUUIDs;
    }
    
    public static List<String> getTeamPlayers(Team team) {
        List<String> teamPlayers = new ArrayList<>();
        for(Map.Entry<String, Team> teamPlayer : getTeamsPlayer().entrySet()) {
            if(teamPlayer.getValue() != team) continue;
            teamPlayers.add(teamPlayer.getKey());
        }
        return teamPlayers;
    }

    public static Team getOppositeTeam(Team team) {
        if(Team.BLUE == team) {
            return Team.RED;
        }
        if(Team.RED == team) {
            return Team.BLUE;
        }
        return null;
    }

    public static void splitTeam(Team team) {
        Team disbandingTeam = getOppositeTeam(team);

        clearTeam(getOppositeTeam(disbandingTeam));
        for(int i = 0; i < getTeamPlayers(team).size()/2; i++) {
            int playerId = new Random().nextInt(getTeamPlayers(team).size()-1);
            String uuid = getTeamPlayers(team).get(playerId+1);

            removePlayerTeam(uuid);
            addPlayerTeam(uuid, disbandingTeam);
        }
    }

    public static boolean isInTeam(Player player) {
       return getTeamsPlayer().containsKey(player.getUniqueId().toString());
    }

    public static Team getTeam(Player player) {
        return getTeamsPlayer().get(player.getUniqueId().toString());
    }
    
}
