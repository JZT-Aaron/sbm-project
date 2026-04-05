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
        Player player = null;
        if(sender instanceof Player playerNew) player = playerNew;

        if(player != null && !player.hasPermission(config.getString("permission.sbm.command.team"))) {
            Info.sendLangError("no-permission", player);
            return true;
        }

        boolean isPlayerNeeded = (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("add"));
        
        Player target = null;
        if(isPlayerNeeded && args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                if(player != null) Info.sendLangError("player-not-found", player, "%player%", args[1]);
                else sender.sendMessage("§cThis Player does not exist.");
                return true;
            }
        }

        if((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) && args.length >= 2) {
            boolean isAdd = args[0].equalsIgnoreCase("add");
            Team team = null;
            if(target == null) throw new IllegalArgumentException("Target can't be null");

            if(!isAdd && !TeamManager.getTeamsPlayer().containsKey(target.getUniqueId().toString())) {
                if(player != null) Info.sendLangError("team.player-not-found", player, "%player%", target.getName());
                else sender.sendMessage("§cThis Player is not in a Team!");
                return true;
            } else if(args.length >= 3) team = Team.getTeamById(args[2]);
            
            if(isAdd) TeamManager.addPlayerTeam(target.getUniqueId().toString(), team);
            else TeamManager.removePlayerTeam(target.getUniqueId().toString());

            String teamId = "team-random";
            if(team != null) teamId = team.getId();

            if(player != null) Info.sendLangInfo("team.team-manuell-change", target, "%player%", target.getName(), "%team%", getText("team." + teamId, player.getUniqueId()), 
            "%state%", getText("state." + (isAdd ? "add" : "remove"), player.getUniqueId()));
            else sender.sendMessage("§aPlayer was removed/added successfully.");
            return true;
        }

        if(player == null) return true;

        // Distribute all Players on the Arena
        if(args[0].equalsIgnoreCase("add-all")) {
            for(Player all : Main.arena.getPlayers()) TeamManager.addPlayerTeam(all.getUniqueId().toString());
            Info.sendInfo("All players were distributed.", "§aDev-Cmd", player);
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
            Info.sendInfo("Team was cleared.", "§aDev-Cmd", player);
            return true;
        }

        Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
        return false;
    }
}
