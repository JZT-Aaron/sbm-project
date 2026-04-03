package de.dachente.sbm.tabs;

import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.Team;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if(args.length == 1) {
            List<String> tab = new ArrayList<>();
            tab.add("add");
            tab.add("add-all");
            tab.add("remove");
            tab.add("clear");
            return tab;
        }

        if(args.length == 2) {
            if(args[0].equalsIgnoreCase("add")) {
                return null;
            }
            List<String> tab = new ArrayList<>();
            if(args[0].equalsIgnoreCase("remove")) {
                for(String uuid : TeamManager.getTeamsPlayer().keySet()) {
                    String name = Bukkit.getPlayer(UUID.fromString(uuid)).getName();
                    tab.add(name); 
                }
                return tab;
            }
            
            for(Team team : Team.values()) {
                tab.add(team.getId());
            }
            return tab;
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("add")) {
            List<String> tab = new ArrayList<>();
            for(Team team : Team.values()) {
                tab.add(team.getId());
            }
            return tab;
        }

        return new ArrayList<>();
    }
}
