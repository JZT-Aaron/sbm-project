package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.Team;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GameCommand implements CommandExecutor {

    FileConfiguration config = Main.getPlugin().getConfig();
    final String SENDER_NAME = "§eGame";
    final String SENDER_NAME_TIMER = "§eTimer";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return true;

        Consumer<String> sendReply = Main.getCmdReplyConsumer(SENDER_NAME, player);
        Consumer<String> sendReplyTimer = Main.getCmdReplyConsumer(SENDER_NAME_TIMER, player);

        if(!player.hasPermission(config.getString("permission.sbm.command.game"))) {
            sendReply.accept("§c§oDies ist dir nicht gestattet!");
            return true;
        }

        if(args.length == 0) {
            sendReply.accept("Bitte benutze §o/game open/close/start/stop/gate [timer <gate-team-name>] [start/stop/set open/close] [<date (yyyy/MM/dd hh:mm:ss)>]");
            return true;
        }

        if(args[0].equalsIgnoreCase("open") && args.length == 1) {
            if(Game.isOpen()) {
                sendReply.accept("§cDas Spiel ist schon offen!");
            }
            Game.open();
            return true;
        }

        if(args[0].equalsIgnoreCase("gate") && args.length >= 2) {
            Team team = args.length == 3 ? Team.getTeamById(args[2]) : null;
            boolean openState;
            if(args[1].equalsIgnoreCase("open") | args[1].equalsIgnoreCase("close")) openState = args[1].equalsIgnoreCase("open") ? true : false; 
            else {
                sendReply.accept("Bitte benutze §7open/close §ofür den zustand der Tore.");
                return true;
            }    
            if(team == null) GateManager.setGateActive(openState);
            else GateManager.setGateActive(openState, team);
        }

        if(args[0].equalsIgnoreCase("close")) {
            if(!Game.isOpen()) {
                sendReply.accept("§cDas Spiel ist schon geschlossen!");
            }
            sendReply.accept("Der Server wurde geschlossen.");
            Game.close();
        }

        if(args[0].equalsIgnoreCase("round")) {
            if(args.length < 2) {
                sendReply.accept("§cBitte benutze round start [now]");
            }
            if(args[1].equalsIgnoreCase("start")) {
                if(Game.isRoundGoing) {
                    sendReply.accept("§cDie Runde läuft schon!");
                    return true;
                }   
                if(args.length == 3 && args[2].equalsIgnoreCase("now")) {
                    Game.beginRound();
                    return true;
                }
                Game.startTimer();
                sendReply.accept("Die Runde wird gestartet.");
            }
            
        }

        // When production add confirm
        if(args[0].equalsIgnoreCase("hard-reset")) {
            if(!Game.isRoundGoing) {
                sendReply.accept("§cDas Spiel läuft nicht!");
                return true;
            }
            sendReply.accept("Das Spiel wird gestoppt.");
            Game.hardReset();
        }

        if(args[0].equalsIgnoreCase("dead") && args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                sendReply.accept("§cDer Spieler ist nicht auf dem Server!");
                return true;
            }
            if(!TeamManager.getTeamsPlayer().containsKey(target.getUniqueId().toString())) {
                sendReply.accept("§cDer Spieler ist in keinem Team!");
                return true;
            }
            Game.deadMode(target);
            sendReply.accept("Der Spieler §7" + Main.toPlain(target.displayName()) + " ist jetzt tod.");
        }

        if(args[0].equalsIgnoreCase("bonus-snowball") && args.length == 2) {
            Team team = Team.getTeamById(args[1]);
            if(team == null) {
                sendReply.accept("Dieses Team gibt es nicht!");
                return true;
            }
            Location l = null;
            if(Team.BLUE == team) {
                l = new Location(Main.arena, 0.5, 3, 7.5);
            }
            if(Team.RED == team) {
                l = new Location(Main.arena, 0.5, 3, -6.5);
            }
            Main.arena.dropItemNaturally(l, new ItemStack(Material.SNOWBALL));
        }

        if(args[0].equalsIgnoreCase("game-joining") && args.length == 2) {
            boolean isOpen = false;

            if(!(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
                Info.sendInfo("Bitte nutzte nur §7on §ooder §7off§o.", SENDER_NAME, player);
                return true;
            }

            if(args[1].equalsIgnoreCase("on")) {
                isOpen = true;
            }

            if(args[1].equalsIgnoreCase("off")) {
                isOpen = false;
            }

            Game.isJoiningOpen = isOpen;
        }

        if(args[0].equalsIgnoreCase("winner") && args.length == 2) {
            Player winner = Bukkit.getPlayer(args[1]);
            if(winner == null) {
                Info.sendInfo("Dieser Spieler ist nicht Online!", player);
                return true;
            }
            Game.winner(winner);
        }

        if(args[0].equalsIgnoreCase("open") && args[1].equalsIgnoreCase("timer")) {
            switch (args[2]) {
                case "start" -> {
                    StartClock.start();
                    sendReplyTimer.accept("Der Timer wurde gestartet.");
                }
                case "stop" -> {
                    StartClock.stop();
                    sendReplyTimer.accept("Der Timer wurde gestoppt.");
                }
                case "set" -> {
                    if(args.length != 5) break;

                    if(!args[3].matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
                        sendReplyTimer.accept("§cBitte nutzte dieses Format: §oyyyy/MM/dd§c!");
                        break;
                    }
                    if(!args[4].matches("\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
                        sendReplyTimer.accept("§cBitte nutzte dieses Format: §ohh:mm:ss§c!");
                        break;
                    }
                    String[] args2 = args[3].split("/");
                    String[] args3 = args[4].split(":");
                    int y = Integer.parseInt(args2[0]);
                    int M = Integer.parseInt(args2[1]);
                    int d = Integer.parseInt(args2[2]);
                    int s = Integer.parseInt(args3[2]);
                    int m = Integer.parseInt(args3[1]);
                    int h = Integer.parseInt(args3[0]);
                    sendReplyTimer.accept("Der Timer wurde auf den §7"+ args[3] +" um " + args[4] + " Uhr §ogesetzt.");
                    config.set("start.clock.time.y", y);
                    config.set("start.clock.time.M", M);
                    config.set("start.clock.time.d", d);
                    config.set("start.clock.time.s", s);
                    config.set("start.clock.time.m", m);
                    config.set("start.clock.time.h", h);
                    Main.getPlugin().saveConfig();
                    if(config.getBoolean("start.clock.started")) {
                        Info.sendImportantInfo("Der Start wurde auf den §o" + args[3] + " §lum §o" + args[4] + "§l verschoben");
                    }
                }
            }
        }
        return false;
    }
}
