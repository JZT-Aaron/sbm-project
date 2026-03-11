package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameServerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        if(!Game.isOpen() && !player.isOp()) {
            Info.sendLangError("server-closed", player);
            return true;
        }

        if(Game.getLivingPlayers().containsKey(player.getUniqueId().toString()) && !player.isOp()) {
            Info.sendLangError("currently-no-permission", player);
            return true;
        }

        Main.joinServer(Server.EVENT_SERVER, player);
        return false;
    }
}
