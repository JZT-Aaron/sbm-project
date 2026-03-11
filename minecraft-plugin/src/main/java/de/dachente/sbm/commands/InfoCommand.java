package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.Info;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
public class InfoCommand implements CommandExecutor {

    FileConfiguration config = Main.getPlugin().getConfig();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        final String SYNTAX = "/info normal/important/private [<player>] <info>";

        if(!player.hasPermission(config.getString("permission.sbm.command.info"))) {
            Info.sendLangError("no-permission", player);
            return true;
        }

        if(args.length < 1) {
            Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
            return true;
        }

        StringBuilder message = new StringBuilder();
        int i = 0;
        for(String arg : args) {
            i += 1;
            if(i <= 1) continue;
            message.append(arg).append(" ");
        }

        if(args[0].equalsIgnoreCase("normal")) {
            Info.sendLangClearInfo(message.toString());
            return true;
        }

        if(args[0].equalsIgnoreCase("important")) {
            Info.sendLangClearImportantInfo(message.toString());
            return true;
        }

        if(args[0].equalsIgnoreCase("private") && args.length > 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                Info.sendLangError("player-not-found", player, "%player%", args[1]);
                return true;
            }
            StringBuilder privateMessage = new StringBuilder();
            int i2 = 0;
            for(String arg : args) {
                i2 += 1;
                if(i2 <= 2) continue;
                privateMessage.append(arg).append(" ");
            }
            Info.sendLangInfo("whisper-to", player, "%player%", target.getName(), "%message%", privateMessage.toString());
            Info.sendLangClearInfo(privateMessage.toString(), target);
            return true;
        }

        Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
        return false;
    }
}
