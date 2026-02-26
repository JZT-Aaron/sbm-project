package de.dachente.sbm.tabs;

import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return getTab(args).stream().filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase())).toList();
    }

    private List<String> getTab(String[] args) {
        if(args.length == 1) {
            List<String> tab = new ArrayList<>();
            if(Game.isOpen()) {
                tab.add("close");
            } else
                tab.add("open");
            if(Game.isRoundGoing) {
                tab.add("stop");
            }
            tab.add("round");
            tab.add("gate");
            tab.add("dead");

            tab.add("bonus-snowball");
            tab.add("game-joining");
            
            return tab;
        }

        if(args.length == 2) {
            List<String> tab = new ArrayList<>();
            if(args[0].equalsIgnoreCase("open")) tab.add("timer");
            if(args[0].equalsIgnoreCase("gate")) {
                tab.add("open");
                tab.add("close");
            }
            if(args[0].equalsIgnoreCase("dead")) {
                for(Player all : Bukkit.getOnlinePlayers()) {
                    if(!Game.getLivingPlayers().contains(all.getUniqueId().toString())) continue;
                    tab.add(all.getName());
                }
            }
            if(args[0].equalsIgnoreCase("next") || args[0].equalsIgnoreCase("bonus-snowballs")) {
                for(Team team : Team.values()) tab.add(team.getId());
            }
            if(args[0].equalsIgnoreCase("game-joining")) {
                tab.add("on");
                tab.add("off");
            }
            if(args[0].equalsIgnoreCase("round")) {
                tab.add("start");
            }
            return tab;
        }

        if(args.length == 3) {
            List<String> tab = new ArrayList<>();
            if(args[0].equalsIgnoreCase("open") && args[1].equalsIgnoreCase("timer")) {
                tab.add("start");
                tab.add("stop");
                tab.add("set");
            }
            if(args[0].equalsIgnoreCase("gate")) {
                for(Team team : Team.values()) {
                    tab.add(team.getId());
                }
            }
            return tab;
        }

        if(args.length == 4) {
            if(args[0].equalsIgnoreCase("open") && args[1].equalsIgnoreCase("timer") && args[2].equalsIgnoreCase("set")) {
                List<String> tab = new ArrayList<>();
                String input = args[3];
                String[] dateParts = input.split("/", -1);
                LocalDateTime now = LocalDateTime.now();
                StringBuilder dateBuilder = new StringBuilder();
                if(dateParts.length > 0) dateBuilder.append(!dateParts[0].isEmpty() ? dateParts[0] : now.getYear()).append("/");
                if(dateParts.length > 1) dateBuilder.append(!dateParts[1].isEmpty() ? dateParts[1] : String.format("%02d", now.getMonthValue())).append("/");
                if(dateParts.length > 2) dateBuilder.append(!dateParts[2].isEmpty() ? dateParts[2] : String.format("%02d", now.getDayOfMonth()));
                tab.add(dateBuilder.toString());
                tab.add(String.format("%02d/%02d/%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
                return tab;
            }
        }

        if(args.length == 5) {
            if(args[0].equalsIgnoreCase("open") && args[1].equalsIgnoreCase("timer") && args[2].equalsIgnoreCase("set") && args[3].matches("\\d{0,4}/\\d{1,2}/\\d{1,2}")) {
                List<String> tab = new ArrayList<>();
                String input = args[4];
                String[] dateParts = input.split(":", -1);
                LocalDateTime now = LocalDateTime.now();
                StringBuilder dateBuilder = new StringBuilder();
                if(dateParts.length > 0) dateBuilder.append(!dateParts[0].isEmpty() ? dateParts[0] : String.format("%02d", now.getHour())).append(":");
                if(dateParts.length > 1) dateBuilder.append(!dateParts[1].isEmpty() ? dateParts[1] : String.format("%02d", now.getMinute())).append(":");
                if(dateParts.length > 2) dateBuilder.append(!dateParts[2].isEmpty() ? dateParts[2] : String.format("%02d", now.getSecond()));
                tab.add(dateBuilder.toString());
                tab.add(String.format("%02d:%02d:", now.getHour(), now.getMinute()));
                tab.add(String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));
                return tab;
            }
        }
    return new ArrayList<>();
    }
}
