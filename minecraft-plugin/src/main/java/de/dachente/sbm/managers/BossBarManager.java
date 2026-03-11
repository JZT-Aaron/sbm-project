package de.dachente.sbm.managers;

import static de.dachente.sbm.managers.LanguageManager.getLanguage;
import static de.dachente.sbm.managers.LanguageManager.getText;
import static de.dachente.sbm.managers.LanguageManager.replacePlaceholdersSmart;

import java.util.HashMap;
import java.util.Map;


import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import de.dachente.sbm.utils.enums.Language;

public class BossBarManager {

    private static Map<Language, BossBar> bossBars = new HashMap<>();
    private static String bossBarTextId;
    private static String[] bossBarPlaceHolders;

    private static BossBar createNewDefaultBossBar(Language language) {
        BossBar bossBar = Bukkit.createBossBar("§7§l" + getText("state.loading", language), BarColor.WHITE, BarStyle.SOLID);
        bossBars.put(language, bossBar);
        return bossBar;
    }

    public static BossBar getBossBar(Language language) {
        if(!bossBars.containsKey(language)) createNewDefaultBossBar(language);
        return bossBars.get(language);
    }

    public static void setTitle(String id) {
        setTitle(id, "");
    }

    public static void setVisible(boolean visible) {
        for(Map.Entry<Language, BossBar> entry : bossBars.entrySet()) entry.getValue().setVisible(visible); 
    }

    public static void addPlayer(Player player) {
        getBossBar(getLanguage(player.getUniqueId())).addPlayer(player);
    }
    
    public static void switchPlayerLang(Player player, Language lang) {
        for(Map.Entry<Language, BossBar> entry : bossBars.entrySet()) entry.getValue().removePlayer(player);
        addPlayer(player);
    }

    public static boolean containsPlayer(Player player) {
        return getBossBar(getLanguage(player.getUniqueId())).getPlayers().contains(player);
    }

    public static void removePlayer(Player player) {
        getBossBar(getLanguage(player.getUniqueId())).removePlayer(player);
    }

    public static void removeAll() {
        for(Map.Entry<Language, BossBar> entry : bossBars.entrySet()) entry.getValue().removeAll(); 
    }

    public static void setTitle(String id, String... placeholders) {
        bossBarTextId = id;
        bossBarPlaceHolders = placeholders;
        updateBossBar();
    }

    public static void updateBossBar() {
        for(Map.Entry<Language, BossBar> entry : bossBars.entrySet()) {
            Language lang = entry.getKey();
            String bossbarTitle = getText("bossbar." + bossBarTextId, lang);
            bossbarTitle = replacePlaceholdersSmart(bossbarTitle, lang, bossBarPlaceHolders);
            entry.getValue().setTitle(bossbarTitle);
        } 
    }
}
