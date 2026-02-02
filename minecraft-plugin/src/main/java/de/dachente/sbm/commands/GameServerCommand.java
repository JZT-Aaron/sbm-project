package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameServerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        if(!Game.isOpen() && !player.isOp()) {
            Game.sendInfo("§cDer Event-Server ist noch nicht Offen!", player);
            return true;
        }

        if(args.length != 0) {
            Game.sendInfo("§oBitte benutze §7/game-server§o!", player);
            return true;
        }

        if(Game.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            Game.sendInfo("Du kannst das jetzt nicht tun!", player);
            return true;
        }

        Game.setGameServerHotbar(player);
        player.teleport(Main.arena.getSpawnLocation());
        return false;
    }
}
