package de.dachente.sbm.managers;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.dachente.sbm.utils.enums.PlayerStat;
import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.PlayerStats;
import net.kyori.adventure.text.Component;

public class StatusManger {
    public static Status getPlayerStatusSync(Player player) {
        return Status.valueOf(PlayerStats.getStringSync(PlayerStat.PLAYERSTATUS, player.getUniqueId()));        
    }

    public static void setPlayerStatus(Status status, Player player) {
        PlayerStats.updateString(PlayerStat.PLAYERSTATUS, player.getUniqueId(), status.toString());
        updatePlayerStatus(status, player);
    }

    public static void updatePlayerStatus(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> updatePlayerStatus(getPlayerStatusSync(player), player));
    }    

    private static void updatePlayerStatus(Status status, Player player) {
        String playerColor = TeamManager.isInTeam(player) ? TeamManager.getTeam(player).getChatColor() : (status == Status.WON ? "§6" : "§f");
        player.playerListName(Component.text(" " + status.getSymbol() + " §7| " + playerColor + Main.toPlain(player.displayName())));
    }

}
