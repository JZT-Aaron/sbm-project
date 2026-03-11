package de.dachente.sbm.managers;

import static de.dachente.sbm.managers.LanguageManager.getText;
import static de.dachente.sbm.managers.LanguageManager.replacePlaceholders;
import static de.dachente.sbm.managers.LanguageManager.replacePlaceholdersSmart;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class Info {
    public enum TitleTime {
        SHORT(Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))),
        NORMAL(Title.Times.times(Duration.ofMillis(700), Duration.ofSeconds(4), Duration.ofMillis(700))),
        LONG(Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(20), Duration.ofMillis(500)));

        private final Title.Times times;

        TitleTime(Title.Times times) {
            this.times = times;
        }

        public Title.Times getTimes() {
            return times;
        }
    }

    public static Consumer<String> getCmdReplyConsumer(String senderName, Player player) {
        return msg -> sendInfo(msg, senderName, player);
    }

    public static Consumer<String> getLangReplyConsumer(String prefixid, String color, Player receiver) {
        return id -> sendLangMessage(id, prefixid, color);
    }

    @FunctionalInterface
    public interface LangReplyConsumer {
        void accept(String id, String... placeholders);
    }

    public static LangReplyConsumer getLangReplyConsumerPh(String prefixid, String color, Player receiver) {
        return (id, placeholders) -> sendLangMessage(id, prefixid, color, placeholders);
    }

    public static void showLangTitle(String id, Player player, TitleTime titleTime, String... placeholders) {
        UUID uuid = player.getUniqueId();
        String text = replacePlaceholders(getText("title." + id + ".title", uuid), false, placeholders);
        String subtitle = replacePlaceholders(getText("title." + id + ".subtitle", uuid), false, placeholders);
        showTitle(text, subtitle, titleTime, player);
    }

    public static void showLangTitle(String id, Player player, String... placeholders) {
        showLangTitle(id, player, TitleTime.NORMAL, placeholders);
    }

    public static void showLangTitle(String id, Player player) {
        showLangTitle(id, player, "");
    }

    public static void showLangTitle(String id, String... placeholders) {
        for(Player all : Bukkit.getOnlinePlayers()) showLangTitle(id, all, placeholders);
    }

    public static void showLangTitle(String id) {
        showLangTitle(id, "");
    }

    public static void showTitle(String text, String subtitle, TitleTime titleTime, Player player) {
        player.showTitle(Title.title(Component.text("§b§l" + text), Component.text("§7" + subtitle), titleTime.getTimes()));
    }

    public static void showTitle(String text, Player player) {
        showTitle(text, "", player);
    }

    public static void showTitle(String title, String subtitle, Player player) {
        showTitle(title, subtitle, TitleTime.NORMAL, player);
    }

    //Change Here Server Message send Format
    public static String getServerMessageFormat(String message, String senderName, String format, UUID uuid) {
        if(format.equals("default")) return getServerMessageFormat(message, senderName);
        if(format.equals("private")) return getServerMessageFormat(message, "§e"+ getText("message-prefix.personal", uuid) +"§7 ◆ §b" + senderName);
        throw new IllegalArgumentException("This message format does not exsist");
    }

    public static String getServerMessageFormat(String message, String senderName) {
        return "§7• §b" + senderName + " §7| >> §o" + message;
    }

    public static void sendLangClearMessage(String text, String prefixId, String color, String messageFormat, Player player) {
        UUID uuid = player.getUniqueId();
        player.sendMessage(getServerMessageFormat(text, color + getText("message-prefix." + prefixId, uuid), messageFormat, uuid));
    }

    public static void sendLangClearInfo(String text) {
        for(Player all : Bukkit.getOnlinePlayers()) sendLangClearMessage(text, "info", "§b", "default", all);
    }

    public static void sendLangClearInfo(String text, Player receiver) {
        sendLangClearMessage(text, "info", "§b", "private", receiver);
    }

    public static void sendLangClearImportantInfo(String text) {
        for(Player all : Bukkit.getOnlinePlayers()) sendLangClearMessage(text, "important-info", "§c", "default", all);
    }

    public static void sendLangMessage(String id, String prefixId, String prefixColor, Player player) {
        UUID uuid = player.getUniqueId();
        sendLangClearMessage(getText(id, uuid), prefixId, prefixColor, "private", player);
    }

    public static void sendLangMessage(String id, String prefixId, String prefixColor) {
        for(Player all : Bukkit.getOnlinePlayers()) {
            UUID uuid = all.getUniqueId();
            sendLangClearMessage(getText(id, uuid), prefixId, prefixColor, "default", all);
        } 
    }

    public static void sendLangMessage(String id, String prefixId, String prefixColor, Player player, String... placeholders) {
        UUID uuid = player.getUniqueId();
        sendLangClearMessage(replacePlaceholdersSmart(getText(id, uuid), uuid, placeholders), prefixId, prefixColor, "private", player);
    }

    public static void sendLangMessage(String id, String prefixId, String prefixColor, String... placeholders) {
        for(Player all : Bukkit.getOnlinePlayers()) {
            UUID uuid = all.getUniqueId();
            sendLangClearMessage(replacePlaceholdersSmart(getText(id, uuid), uuid, placeholders), prefixId, prefixColor, "default", all);
        } 
    }

    public static void sendLangError(String error, Player receiver) {
        sendLangError(error, receiver, "");
    }

    public static void sendLangError(String error, Player receiver, String... placeholders) {
        sendLangMessage("error." + error, "error", "§c", receiver, placeholders);
    }

    public static void sendLangInfo(String id, Player receiver, String... placeholders) {
        sendLangMessage("info." + id, "info", "§b", receiver, placeholders);
    }

    public static void sendLangInfo(String id, Player receiver) {
        sendLangInfo(id, receiver, "");
    }

    public static void sendLangInfo(String id, String... placeholders) {
        sendLangMessage("info." + id, "info", "§b", placeholders);
    }

    public static void sendLangInfo(String id) {
        sendLangInfo(id, "");
    }

    public static void sendLangImportantInfo(String id, Player receiver, String... placeholders) {
        sendLangMessage("info." + id, "important-info", "§c", receiver, placeholders);
    }

    public static void sendLangImportantInfo(String id, Player receiver) {
        sendLangImportantInfo(id, receiver, "");
    }

    public static void sendLangImportantInfo(String infoId, String... placeholders) {
        sendLangMessage("info." + infoId, "important-info", "§c", placeholders);
    }

    public static void sendLangImportantInfo(String infoId) {
        sendLangImportantInfo(infoId, "");
    }

    //Normal

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
        sendInfo(message, "Info", receiver);
    }

    public static void sendInfo(String message, String senderName, Player receiver) {
        receiver.sendMessage(getServerMessageFormat(message, senderName, "private", receiver.getUniqueId()));
    }
}
