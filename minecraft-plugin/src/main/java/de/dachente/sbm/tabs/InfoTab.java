package de.dachente.sbm.tabs;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InfoTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if(args.length == 1) {
            List<String> tab = new ArrayList<>();
            tab.add("normal");
            tab.add("important");
            tab.add("private");
            return tab;
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("private")) {
            List<String> tab = new ArrayList<>();
            for(Player all : Bukkit.getOnlinePlayers()) {
                tab.add(all.getName());
            }
            return tab;
        }

        return new ArrayList<>();
    }
}
