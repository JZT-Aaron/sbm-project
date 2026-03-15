package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.Team;

import static de.dachente.sbm.managers.LanguageManager.getText;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {

    FileConfiguration config = Main.getPlugin().getConfig();
    String SYNTAX = "/team add/remove/clear [<team>] [<player>]";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        if(!player.hasPermission(config.getString("permission.sbm.command.team"))) {
            Info.sendLangError("no-permission", player);
            return true;
        }

        boolean isPlayerNeeded = (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("add"));
        
        Player target = null;
        if(isPlayerNeeded && args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                Info.sendLangError("player-not-found", player, "%player%", args[1]);
                return true;
            }
        }

        if((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) && args.length >= 2) {
            boolean isAdd = args[0].equalsIgnoreCase("add");
            Team team = null;
            if(target == null) throw new IllegalArgumentException("Target can't be null");

            if(!isAdd && !TeamManager.getTeamsPlayer().containsKey(target.getUniqueId().toString())) {
                Info.sendLangError("team.player-not-found", target, "%player%", target.getName());
                return true;
            } else if(args.length >= 3) team = Team.getTeamById(args[2]);
            
            if(isAdd) TeamManager.addPlayerTeam(target.getUniqueId().toString(), team);
            else TeamManager.removePlayerTeam(target.getUniqueId().toString());

            String teamId = "team-random";
            if(team != null) teamId = team.getId();

            Info.sendLangInfo("team.team-manuell-change", target, "%player%", target.getName(), "%team%", getText("team." + teamId, player.getUniqueId()), 
            "%state%", getText("state." + (isAdd ? "add" : "remove"), player.getUniqueId()));
            return true;
        }

        if(args[0].equalsIgnoreCase("add-all")) {
            for(Player all : Main.arena.getPlayers()) TeamManager.addPlayerTeam(all.getUniqueId().toString());
            Info.sendInfo("All players were distributed.", "§aDev-Cmd");
            return true;
        }

        if(args[0].equalsIgnoreCase("clear") && args.length >= 2) {
            Team team = Team.getTeamById(args[1]);
            if(team == null) {
                Info.sendLangError("team.not-found", target);
                return true;
            }
            if(TeamManager.getTeamPlayers(team).size() <= 0) {
                Info.sendLangError("team.already-empty", player);
                return true;
            }
            for(String teamPlayer : TeamManager.getTeamPlayers(team)) {
                TeamManager.removePlayerTeam(teamPlayer);
            }
            Info.sendInfo("Team was cleared.", "§aDev-Cmd");
            return true;
        }

        Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
        return false;
    }
}
