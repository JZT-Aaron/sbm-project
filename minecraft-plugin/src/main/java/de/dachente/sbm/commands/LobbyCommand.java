package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.enums.Server;

import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        Consumer<String> sendReply = Main.getCmdReplyConsumer("§eBefehle", player);

        if(args.length != 0) {
            sendReply.accept("Bitte benutzte nur §7/lobby§o.");
            return true;
        }

        if(TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString())) {
            sendReply.accept("Du kannst das jetzt nicht tun!");
            return true;
        }

        Main.joinServer(Server.LOBBY, player);
        return false;
    }
}
