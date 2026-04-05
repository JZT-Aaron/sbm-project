package de.dachente.sbm.commands;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.managers.Info;
import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.managers.TeamManager;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Team;

import static de.dachente.sbm.managers.LanguageManager.getText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.google.common.primitives.Ints;

public class GameCommand implements CommandExecutor {

    FileConfiguration config = Main.getPlugin().getConfig();
    final String SENDER_NAME = "§eGame";
    final String SENDER_NAME_TIMER = "§eTimer";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if(sender instanceof Player) player = (Player) sender; 
        final String SYNTAX = "/game open/close/round/gate/game-joining/start/pause/resume/reset [timer open/close on/off] [start/stop/set open/close ] [<date> <time>]";
        final String CONSOLE_SYNTAX = "/game start/pause/resume/reset/open/close/game-joining [<on|off>]";

        //No Permission Check for Sender that is not Player
        if(player != null && !player.hasPermission(config.getString("permission.sbm.command.game"))) {
            Info.sendLangError("no-permission", player);
            return true;
        }

        //Paramerters are allways requiered
        if(args.length == 0) {
            if(player != null) Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
            else sender.sendMessage("[Error] Please use " + CONSOLE_SYNTAX);
            return true;
        }

        //Start Round
        if(args[0].equalsIgnoreCase("start")) {
            if((TeamManager.getTeamPlayers(Team.RED).size()*TeamManager.getTeamPlayers(Team.BLUE).size()) < 1) {
                if(player != null) Info.sendLangError("game.not-enough-players", player);
                else sender.sendMessage("§c[Error] Not Enough Players!");
                return true;
            }
            if(Game.isRunning()) {
                if(player != null) Info.sendLangError("game.round-already", player,  "%state%",LanguageManager.getText("state.started", player.getUniqueId()));
                else sender.sendMessage("§c[Error] Round already started");
                return true;
            }
            // Dev-Cmd: Skip Game Start Timer
            if(args.length == 2 && args[1].equalsIgnoreCase("now")) {
                Game.beginRound();
                return true;
            }
            Game.startTimer();
            if(player != null) Info.sendLangInfo("game.game-state-change", player, "%state%", LanguageManager.getText("state.started", player.getUniqueId()));
            else sender.sendMessage("§aGame started.");
            return true;
        }

        if(args[0].equalsIgnoreCase("resume")) {
            if(!Game.state().equals(GameState.PAUSED)) {
                if(player != null) Info.sendLangError("game.round-not-already", player,  "%state%",LanguageManager.getText("state.paused", player.getUniqueId()));
                sender.sendMessage("§c[Error] Game is already resumed");
                return true;
            }
            Game.resume();
            if(player != null) Info.sendLangInfo("game.game-state-change", player, "%state%", LanguageManager.getText("state.resumed", player.getUniqueId()));
            else sender.sendMessage("§a[Info] Game was resumed");
            return true;
        }

        //Normal Reset
        if(args[0].equalsIgnoreCase("reset")) {
            Game.resetRound();
            if(player != null) Info.sendInfo("Reset Complete", "§aDev-Cmd", player);
            else sender.sendMessage("§a[Info] Reset Complete");
            return true;
        }
        
        if(args[0].equalsIgnoreCase("pause")) {
            if(!Game.isRunning()) {
                if(player != null) Info.sendLangError("game.round-not-already", player,  "%state%",LanguageManager.getText("state.started", player.getUniqueId()));
                else sender.sendMessage("§c[Error] Game is already paused");
                return true;
            }
            Game.pause();
            if(player != null) Info.sendLangInfo("game.game-state-change", player, "%state%", LanguageManager.getText("state.paused", player.getUniqueId()));
            else sender.sendMessage("§a[Info] Game is Paused");
            return true;
        }

        //Allows Joining by the Hotbar Items to Teams
        if(args[0].equalsIgnoreCase("game-joining") && args.length == 2) {
            if(!(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
                if(player != null) Info.sendLangError("syntax-error", player, "%syntax%", "/game game-joining on/off");
                else sender.sendMessage("§c[Error] Please use /game game-joining on/off");
                return true;
            }
            boolean isOn = args[1].equalsIgnoreCase("on");
            Game.setGameStatus(isOn ? GameState.OPEN : GameState.CLOSED);
            if(player != null) Info.sendLangInfo("game.game-joining", player, "%state%", getText("state." + (isOn ? "unlocked" : "locked"), player.getUniqueId()));
            else sender.sendMessage("§aGame-Joining Changed");
            return true;
        }


        // Open/Close Game-Server
        if((args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("close")) && args.length == 1) {
            boolean requestOpen = args[0].equalsIgnoreCase("open");
            UUID uuid = player == null ? null : player.getUniqueId();
            String stateId = "state." + (requestOpen ? "opened" : "closed");
            if(Game.isOpen() == requestOpen) {
                if (player != null) Info.sendLangError("game.game-already", player,  "%state%",LanguageManager.getText(stateId, uuid));
                else sender.sendMessage("Game-Server is already opened/closed");
                return true;
            }
            if(requestOpen) Game.open();
            else Game.close();
            if(player != null) Info.sendLangInfo("server-state-change", player, "%state%", LanguageManager.getText(stateId, uuid));
            else sender.sendMessage("Game-Server opened/closed");
            return true;
        }


        //From here just Players (No Console) 
        if(player == null) {
            sender.sendMessage("§c[Error] Please use " + CONSOLE_SYNTAX);
            return true;
        } 

        //End Game Early
        if(args[0].equalsIgnoreCase("end")) {
            if(!Game.hasStarted()) {
                Info.sendLangError("game.round-not-already", player, "%state%", getText("state.started", player.getUniqueId()));
                return true;
            }
            Game.endRound();
            Info.sendInfo("Match Ended", "§aDev-Cmd", player);
            return true;
        }

        if(args[0].equalsIgnoreCase("gate") && (args[1].equalsIgnoreCase("open") || args[1].equalsIgnoreCase("close")) && args.length >= 2) {
            Team team = args.length == 3 ? Team.getTeamById(args[2]) : null;
            UUID uuid = player.getUniqueId();
            boolean openState = args[1].equalsIgnoreCase("open");   
            List<String> placeholder = new ArrayList<>();
            placeholder.add("%state%");
            placeholder.add(LanguageManager.getText("state." + (openState ? "unlocked" : "locked"), uuid));
            if(team == null) GateManager.setGateActive(openState);
            else {
                placeholder.add("%team%");
                placeholder.add(LanguageManager.getText("team." + (team.getId()), uuid));
                GateManager.setGateActive(openState, team);
            }
            Info.sendLangInfo("game." + (team == null ? "gate-change" : "team-gate-change"), player, placeholder.toArray(String[]::new));
            return true;
        }
                
        //Dev-Cmd Complete Reset
        if(args[0].equalsIgnoreCase("hard-reset")) {
            Game.hardReset();
            Info.sendInfo("Hard Reset Complete", "§aDev-Cmd", player);
            return true;
        }

        if(args[0].equalsIgnoreCase("dead") && args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                Info.sendLangError("player-not-found", player, "%player%", args[1]);
                return true;
            }
            if(!TeamManager.getTeamsPlayer().containsKey(target.getUniqueId().toString())) {
                Info.sendLangError("team.player-not-found", player, "%player%", target.getName());
                return true;
            }
            Game.deadMode(target);
            Info.sendInfo("Player killed", "§aDev-Cmd", player);
            return true;
        }

        if(args[0].equalsIgnoreCase("bonus-snowball") && args.length >= 2) {
            Team team = Team.getTeamById(args[1]);
            int amount = 1;
            if(args.length >= 3) {
                Integer parsed = Ints.tryParse(args[2]);
                if(parsed == null) {
                    Info.sendLangError("syntax-error", player, "%syntax%", "/game bonus-snowball <amount>");
                    return true;
                }
                amount = parsed;
            }

            if(team == null) {
                Info.sendLangError("team.not-found", player);
                return true;
            }

            Game.dropBonusSnowball(team, amount);
            Info.sendInfo("Snowball dropped", "§aDev-Cmd", player);
            return true;
        }

        

        if(args[0].equalsIgnoreCase("winner") && args.length == 2) {
            Player winner = Bukkit.getPlayer(args[1]);
            if(winner == null) {
                Info.sendLangError("player-not-found", player, "%player%", args[1]);
                return true;
            }
            Game.winner(winner);
            Info.sendInfo("Player set as won.", "§aDev-Cmd", player);
            return true;
        }

        if(args[0].equalsIgnoreCase("open") && args[1].equalsIgnoreCase("timer") && args.length >= 3) {
            switch (args[2]) {
                case "start" -> {
                    StartClock.start();
                    Info.sendLangInfo("timer.started", player);
                }
                case "stop" -> {
                    StartClock.stop();
                    Info.sendLangInfo("timer.stopped", player);
                }
                case "set" -> {
                    if(args.length != 5) {
                        Info.sendLangError("syntax-error", player, "%syntax%", "/game open timer set <date> <time>");
                        break;
                    };

                    if(!args[3].matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
                        Info.sendLangError("timer.wrong-format", player, "%format%","yyyy/MM/dd");
                        break;
                    }
                    if(!args[4].matches("\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
                        Info.sendLangError("timer.wrong-format", player, "%format%", "hh:mm:ss");
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
                    Info.sendLangInfo("timer.set", player, "%date%", args[3], "%time%", args[4]);
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
                default -> {
                    Info.sendLangError("syntax-error", player, "%syntax%", "/game open timer start/stop/set [<date> <time>]");
                }
            }
            return true;
        }
        Info.sendLangError("syntax-error", player, "%syntax%", SYNTAX);
        return false;
    }
}
