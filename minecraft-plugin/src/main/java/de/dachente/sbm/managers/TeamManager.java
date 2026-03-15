package de.dachente.sbm.managers;

import static de.dachente.sbm.managers.LanguageManager.getText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.GameStat;
import de.dachente.sbm.utils.GameStats;
import de.dachente.sbm.utils.enums.Team;

public class TeamManager {
    
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
        if(team == null) {
            addPlayerTeam(uuid);
            return;
        } 
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        player.getInventory().clear();
        //Location teamSpawnLocation = null;

        Map<String, Team> teamsPlayer = getTeamsPlayer(); 
        teamsPlayer.put(uuid, team);
        updateTeamsPlayers(teamsPlayer);

        player.teleport(team.getTeamSpawnLocation());

        Game.loadLobbyInv(player);
        player.updateInventory();

        setTeamChestPlate(team, player);
        UUID rUuid = UUID.fromString(uuid);
        Info.sendLangInfo("team.team-join", Bukkit.getPlayer(rUuid), "%team%", getText("team." + team.getId(), rUuid));
    }

    public static void removePlayerTeam(String uuid) {
        Map<String, Team> teamsPlayer = getTeamsPlayer(); 
        Team team = teamsPlayer.get(uuid);
        if(team == null) return;

        Player player = Bukkit.getPlayer(UUID.fromString(uuid));

        teamsPlayer.remove(uuid);
        updateTeamsPlayers(teamsPlayer);

        Game.setViewer(player);
        UUID rUuid = UUID.fromString(uuid);
        Info.sendLangInfo("team.team-leave", Bukkit.getPlayer(rUuid), "%team%", getText("team." + team.getId(), rUuid));
    }


    public static void clearTeam(Team team) {
        for(String uuid : getTeamPlayers(team)) {
            removePlayerTeam(uuid);
        }
    }

    public static void setTeamChestPlate(Team team, Player player) {
        ItemStack teamChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta teamChestplateMeta = (LeatherArmorMeta) teamChestplate.getItemMeta();
        teamChestplateMeta.setColor(team.getColor());
        teamChestplateMeta.getPersistentDataContainer().set(Main.NO_MOVE, PersistentDataType.BYTE, (byte) 1);
        teamChestplateMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        teamChestplateMeta.addItemFlags(ItemFlag.HIDE_DYE);
        teamChestplate.setItemMeta(teamChestplateMeta);
        player.getInventory().setChestplate(teamChestplate);
    }
    
    public static void setTeamChestPlate(Player player) {
        Team team = TeamManager.getTeamsPlayer().get(player.getUniqueId().toString());
        setTeamChestPlate(team, player);
    }

    public static Map<String, Team> getTeamsPlayer() {
        return GameStats.get(GameStat.TEAM_PLAYERS);
    }

    public static void updateTeamsPlayers(Map<String, Team> teamsPlayer) {
        GameStats.set(GameStat.TEAM_PLAYERS, teamsPlayer);
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

        clearTeam(disbandingTeam);
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

    public static Team getTeam(String uuid) {
        return getTeamsPlayer().get(uuid);
    }
    
}
