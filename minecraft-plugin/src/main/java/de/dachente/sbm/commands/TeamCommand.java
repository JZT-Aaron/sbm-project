package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Team;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {

    FileConfiguration config = Main.getPlugin().getConfig();
    String SYNTAX = "§oBitte benutzte §7/team add/remove/clear [<team>] [<player>]§o.";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        Consumer<String> sendReply = Main.getCmdReplyConsumer("§eBefehle", player);

        if(!player.hasPermission(config.getString("permission.sbm.command.team"))) {
            sendReply.accept("§c§oDies ist dir nicht gestattet!");
            return true;
        }

        boolean isTeamNeeded = (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("clear"));

        int playerState = -10;
        if(args[0].equalsIgnoreCase("remove")) playerState = 1;
        else if(args[0].equalsIgnoreCase("add")) playerState = 2;

        Team team = null;
        if(isTeamNeeded)
            for(Team aTeam : Team.values()) {
                String id  = aTeam.getId();
                if(!id.equalsIgnoreCase(args[1])) continue;
                team = aTeam;
            }

        Player target = null;
        if(args.length >= playerState+1 && playerState > 0) {
            target = Bukkit.getPlayer(args[playerState]);
            if(target == null) {
                sendReply.accept("§c§oDieser Spieler ist nicht Online!");
                return true;
            }
        }

        if(team == null && isTeamNeeded) {
            sendReply.accept("§c§oDieses Team gibt es nicht!");
            return true;
        }

        if(target == null) {
            target = player;
        }

        if(args[0].equalsIgnoreCase("add")) {
            TeamManager.addPlayerTeam(target.getUniqueId().toString(), team);
            return true;
        }

        if(args[0].equalsIgnoreCase("add-all")) {
            for(Player all : Main.arena.getPlayers()) TeamManager.addPlayerTeam(all.getUniqueId().toString());;
        }

        if(args[0].equalsIgnoreCase("remove-all")) {
            for(Player all : Main.arena.getPlayers()) TeamManager.removePlayerTeam(all.getUniqueId().toString());;
        }

        if(args[0].equalsIgnoreCase("clear")) {
            if(TeamManager.getTeamPlayers(team).size() <= 0) {
                sendReply.accept("§c§oDas §7Team §oist bereits §cleer§o!");
                return true;
            }
            for(String teamPlayer : TeamManager.getTeamPlayers(team)) {
                TeamManager.removePlayerTeam(teamPlayer);
            }
            return true;
        }

        if(args[0].equalsIgnoreCase("remove")) {
            if(!TeamManager.getTeamsPlayer().containsKey(target.getUniqueId().toString())) {
                sendReply.accept("§c§oDer Spieler §c" + target.getName() + " §oist in keinem §cTeam§o!");
                return true;
            }
            TeamManager.removePlayerTeam(target.getUniqueId().toString());
            return true;
        }

        sendReply.accept(SYNTAX);
        return false;
    }
}
