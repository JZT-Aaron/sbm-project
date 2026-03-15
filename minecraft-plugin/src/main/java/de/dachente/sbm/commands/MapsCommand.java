package de.dachente.sbm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.MapManager;
import de.dachente.sbm.utils.enums.GameMap;

public class MapsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command arg1, @NotNull String arg2, String[] args) {
        if(!(sender instanceof Player player)) return true;

        if(args[0].equalsIgnoreCase("update") && args.length == 1) {
            MapManager.updateMapsNbtFiles();
            Info.sendLangInfo("map.updated", player);
            return true;
        }

        if(args[0].equalsIgnoreCase("load") && args.length == 2) {
            GameMap map = GameMap.fromId(args[1]);
            if(map == null) {
                Info.sendLangError("map.not-found", player);
                return true;
            }
            MapManager.loadMap(map);
            Info.sendLangInfo("map.loaded", player, "%map%", map.getId());
            return true;
        }

        return false;
    }
    
}
