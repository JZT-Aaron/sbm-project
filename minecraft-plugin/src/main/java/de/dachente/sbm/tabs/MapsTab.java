package de.dachente.sbm.tabs;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.dachente.sbm.utils.enums.GameMap;

public class MapsTab implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender arg0, @NotNull Command arg1, @NotNull String arg2, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();

        if(args.length == 1) {
            tab.add("update");
            tab.add("load");
        }

        if(args[0].equalsIgnoreCase("load") && args.length == 2) {
            for(GameMap map : GameMap.values()) tab.add(map.getId());
        }

        return tab.stream().filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase())).toList();
    }
    
}
