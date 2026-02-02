package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;

import java.util.function.Consumer;

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

        Consumer<String> sendReply = Main.getCmdReplyConsumer("§eBefehle", player);

        if(!player.hasPermission(config.getString("permission.sbm.command.info"))) {
            sendReply.accept("§c§oDies ist dir nicht gestattet!");
            return true;
        }

        if(args.length < 1) {
            sendReply.accept("Bitte benutze §o/info normal/important/private [<player>] <info>");
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
            Game.sendInfo(message.toString());
            return true;
        }

        if(args[0].equalsIgnoreCase("important")) {
            Game.sendImportantInfo(message.toString());
            return true;
        }

        if(args[0].equalsIgnoreCase("private") && args.length > 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                sendReply.accept("Diesen Spieler gibt es nicht!");
                return true;
            }
            StringBuilder privateMessage = new StringBuilder();
            int i2 = 0;
            for(String arg : args) {
                i2 += 1;
                if(i2 <= 2) continue;
                privateMessage.append(arg).append(" ");
            }
            sendReply.accept("§7§oDu flüstert zu " + Main.toPlain(player.displayName()) + "§7: " + privateMessage.toString());
            Game.sendInfo(privateMessage.toString(), target);
            return true;
        }

        sendReply.accept("Bitte benutze §o/info normal/important/private [<player>] <info>");
        return false;
    }
}
