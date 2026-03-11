package de.dachente.sbm.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.ItemBuilder;
import de.dachente.sbm.utils.PlayerStats;
import de.dachente.sbm.utils.StartClock;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.Server;
import de.dachente.sbm.utils.enums.Status;
import net.kyori.adventure.text.Component;

public class LanguageManager {

    private static Map<UUID, Language> playerLanguages = new HashMap<>();

    public static ItemStack getLanguageChangeItem(UUID uuid) {
        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThkYWExZTNlZDk0ZmYzZTMzZTFkNGM2ZTQzZjAyNGM0N2Q3OGE1N2JhNGQzOGU3NWU3YzkyNjQxMDYifX19";
        String url = "http://textures.minecraft.net/texture/98daa1e3ed94ff3e33e1d4c6e43f024c47d78a57ba4d38e75e7c9264106";
        ItemStack changeLanguage = new ItemBuilder(Material.PLAYER_HEAD).setLangNameDescriptionTag("open-change-language", uuid).setSkullTexture(texture, url).setUnmovable().build();
        return changeLanguage;
    }

    public static void loadLang() {
        for(Language lang : Language.values()) {
            lang.setFile(getLangFile(lang.getFileName()));
        }
    }

    private static FileConfiguration getLangFile(String lang) {    
        String path = "lang/lang_" + lang + ".yml";
        File file = new File(Main.getPlugin().getDataFolder(), path);
        
        if(!file.exists()) {
            Main.getPlugin().saveResource(path, false);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public static void openLanguageMenu(Player player) {
        Language lang = playerLanguages.get(player.getUniqueId());
        int slots = ((Language.values().length + 8) / 9) * 9;
        Inventory inv = Bukkit.createInventory(null, slots, Component.text(getMenuTitle("change-lang", player)));
        UUID uuid = player.getUniqueId();
        for(Language cLang : Language.values()) {
            ItemStack scull = cLang.getScull();
            if(lang.equals(cLang))
                scull = new ItemBuilder(scull).setMaterial(Material.NETHER_STAR).setDisplayName(scull.displayName().append(Component.text(" §7§o("+ getText("state.selected", uuid) +")"))).addEnchant(Enchantment.LUCK_OF_THE_SEA, 1).addFlags(ItemFlag.HIDE_ENCHANTS).build();
        
            inv.addItem(scull);
        }
        player.openInventory(inv);
    }

    public static void addOnlineSnyc(UUID userUuid) {
        playerLanguages.put(userUuid, PlayerStats.getLanguageSync(userUuid));
    }

    public static void removeOnline(UUID userUuid) {
        playerLanguages.remove(userUuid);
    }

    public static void setLanguage(Player player, Language lang) {
        boolean hasBossbar = BossBarManager.containsPlayer(player);
        if(hasBossbar) BossBarManager.removePlayer(player);

        PlayerStats.updateLang(player.getUniqueId(), lang);
        playerLanguages.put(player.getUniqueId(), lang);

        if(hasBossbar) BossBarManager.addPlayer(player);
        Server cServer = null;

        
        for(Server server : Server.values()) {
            if(!server.getWorld().equals(player.getWorld())) continue;
            cServer = server;
            break;
        }

        if(player.getWorld().equals(Main.lobby)) StartClock.updateSigns(player, false);

        if(cServer == Server.EVENT_SERVER) {
            if(Game.getLivingPlayers().containsKey(player.getUniqueId().toString())) return;
            if(TeamManager.getTeamsPlayer().containsKey(player.getUniqueId().toString()) && StatusManger.getPlayerStatus(player).equals(Status.WAITING)) {
                Game.loadLobbyInv(player);
                return;
            }
        }    
        Game.setServerHotbar(cServer, player);
        
    }

    public static Language getLanguage(UUID userUUID) {
        return playerLanguages.get(userUUID);
    }

    public static boolean hasLanguage(UUID userUuid, Language language) {
        return getLanguage(userUuid).equals(language);
    }

    public static String getText(String id, Language lang) {
        return lang.getFile().getString(id, id);
    }

    public static String getText(String id, UUID userUuid) {
        return getText(id, getLanguage(userUuid));
    }

    public static String getMenuTitle(String menu, Player player) {
        return "§a§l" + getLanguage(player.getUniqueId()).getFile().getString("menu." + menu + ".title");
    }

    public static String getItemName(String item, UUID uuid) {
        ConfigurationSection itemKey = getLanguage(uuid).getFile().getConfigurationSection("items." + item);
        String click = itemKey.getBoolean("no-click", false) ? "" : " §7§o" + getText("info.click", uuid);
        return "§f§l" + itemKey.getString("name") + click;
    }

    public static String getMessagePrefix(String prefix, UUID uuid) {
        return getText("message-prefix." + prefix, uuid);
    }

    public static List<String> getItemDescription(String item, UUID uuid) {
        List<String> lines = new ArrayList<>();
        Language lang = getLanguage(uuid);
        lines = TextSplitter.split(lang.getFile().getString("items." + item + ".description"), lang.getLocale(), 26);
        lines = lines.stream().map(line -> "§7" + line).toList();
        return lines;
    }

    public static String replacePlaceholdersSmart(String text, Language language, String... placeholders) {
        return replacePlaceholdersSmart(text, language, true, placeholders);
    }

    public static String replacePlaceholdersSmart(String text, UUID uuid, String... placeholders) {
        return replacePlaceholdersSmart(text, getLanguage(uuid), placeholders);
    }

    public static String replacePlaceholdersSmart(String text, Language language, Boolean changeColors, String... gPlaceholders) {
        if(gPlaceholders[0].isEmpty()) return text;
        String[] placeholders = gPlaceholders.clone();
        if(text == null || placeholders.length % 2 != 0) throw new IllegalArgumentException("Please use right Argurments for Placeholders");
        for(int i = 1; i < placeholders.length; i+=2) {
            if(!placeholders[i].startsWith("@")) continue;
            placeholders[i] = getText(placeholders[i].replace("@", ""), language);
        }
        text = replacePlaceholders(text, changeColors, placeholders);
        return text;
    }

    public static String replacePlaceholders(String text, String... placeholders) {
        return replacePlaceholders(text, true, placeholders);
    }

    public static String replacePlaceholders(String text, Boolean changeColors, String... placeholders) {
        if(placeholders[0].isEmpty()) return text;
        if(text == null || placeholders.length % 2 != 0) throw new IllegalArgumentException("Please use right Argurments for Placeholders");
        for(int i = 0; i < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], (changeColors ? "§7" : "") + placeholders[i+1] + (changeColors ? "§7§o" : ""));
        }
        return text;
    }


}
