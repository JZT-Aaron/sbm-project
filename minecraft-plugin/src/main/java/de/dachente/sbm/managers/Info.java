package de.dachente.sbm.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.dachente.sbm.utils.Game.TitleTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class Info {
    public static void showTitle(String text, String subtitle, TitleTime titleTime, Player player) {
        player.showTitle(Title.title(Component.text(text), Component.text(subtitle), titleTime.getTimes()));
    }

    public static void showTitle(String text, Player player) {
        showTitle(text, "", player);
    }

    public static void showTitle(String title, String subtitle, Player player) {
        showTitle(title, subtitle, TitleTime.NORMAL, player);
    }

    //Change Here Server Message send Format
    public static String getServerMessageFormat(String message, String senderName) {
        return "§7• §b" + senderName + " §7| >> §o" + message;
    }

    public static void broadcastMessage(String message) {
        Bukkit.broadcast(Component.text(message));
    }

    public static void sendInfo(String message) {
        sendInfo(message, "Info");
    }

    public static void sendInfo(String message, String senderName) {
        broadcastMessage(getServerMessageFormat(message, senderName));
    }

    public static void sendImportantInfo(String message) {
        sendInfo(message, "§cWichtige Info");
    }

    public static void sendInfo(String message, Player receiver) {
        sendInfo(message, "Info §7◆ §ePrivate§7", receiver);
    }

    public static void sendInfo(String message, String senderName, Player receiver) {
        receiver.sendMessage(getServerMessageFormat(message, senderName));
    }
}
