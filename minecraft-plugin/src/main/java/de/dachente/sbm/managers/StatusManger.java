package de.dachente.sbm.managers;

import java.util.Map;

import org.bukkit.entity.Player;

import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.GameStat;
import de.dachente.sbm.utils.GameStats;
import net.kyori.adventure.text.Component;

public class StatusManger {
    public static Status getPlayerStatus(Player player) {
        return getPlayerStatus().get(player.getUniqueId().toString());
    }

    public static Map<String, Status> getPlayerStatus() {
        return GameStats.get(GameStat.PLAYER_STATUS);
    }

    public static void setPlayerStatus(Status status, Player player) {
        Map<String, Status> playerStatus = getPlayerStatus();
        playerStatus.put(player.getUniqueId().toString(), status);
        GameStats.set(GameStat.PLAYER_STATUS, playerStatus);
        updatePlayerStatus(status, player);
    }

    public static void updatePlayerStatus(Player player) {
        updatePlayerStatus(getPlayerStatus(player), player);
    }    

    private static void updatePlayerStatus(Status status, Player player) {
        String playerColor = TeamManager.isInTeam(player) ? TeamManager.getTeam(player).getChatColor() : (status == Status.WON ? "§6" : "§f");
        player.playerListName(Component.text(" " + status.getSymbol() + " §7| " + playerColor + Main.toPlain(player.displayName())));
    }

}
