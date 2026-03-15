package de.dachente.sbm.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(Material material, ItemMeta meta) {
        this.itemStack = new ItemStack(material);
        itemStack.setItemMeta(meta);
        this.meta = meta;
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder setMaterial(Material material) {
        return new ItemBuilder(this.build().withType(material));
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder setDisplayName(Component component) {
        meta.displayName(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder setLangNameDescriptionTag(String itemid, UUID uuid) {
        this.setTagData(itemid);
        this.setLangNameDescription(itemid, uuid);
        return this;
    }

    public ItemBuilder setLangNameDescription(String itemid, UUID uuid) {
        this.setDisplayName(LanguageManager.getItemName(itemid, uuid));
        this.setLangDescription(itemid, uuid);
        return this;
    }

    public ItemBuilder setLangDescription(String itemid, UUID uuid) {
        this.setLore(LanguageManager.getItemDescription(itemid, uuid).toArray(String[]::new));
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        return this;
    }


    // Set Lore / Description
    public ItemBuilder setLore(String... loreLines) {
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder setSkullOwner(String playername) {
        if(!(meta instanceof SkullMeta skullMeta)) return this;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playername));
        return this;
    }

    public ItemBuilder setSkullTexture(String textureValue) {
        if(!(meta instanceof SkullMeta skullMeta)) return this;
        UUID uuid = new UUID(textureValue.hashCode(), textureValue.hashCode());
        PlayerProfile profile = Bukkit.createProfile(uuid, null);
        profile.getProperties().add(new ProfileProperty("textures", textureValue));
        skullMeta.setPlayerProfile(profile);
        return this;
    }

    public ItemBuilder setSkullTexture(String textureValue, String url) {
        if(!(meta instanceof SkullMeta skullMeta)) return this;

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(new ProfileProperty("textures", encodeBase64(url)));
        skullMeta.setPlayerProfile(profile);
        return this;
    }

    private String encodeBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    public ItemBuilder addFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setPotionType(PotionType potionType) {
        if(!(meta instanceof PotionMeta potionMeta)) return this;
        potionMeta.setBasePotionType(potionType);
        return this;
    }

    public ItemBuilder setPotionEffect(PotionEffect potionEffect) {
        if(!(meta instanceof PotionMeta potionMeta)) return this;
        potionMeta.addCustomEffect(potionEffect, true);
        return this;
    }

    // Add No-Move-Tag
    public ItemBuilder setUnmovable() {
        meta.getPersistentDataContainer().set(Main.NO_MOVE, PersistentDataType.BYTE, (byte) 1);
        return this;
    }

    public ItemBuilder setMovable() {
        meta.getPersistentDataContainer().remove(Main.NO_MOVE);
        return this;
    }

    public ItemBuilder setUnDroppable() {
        meta.getPersistentDataContainer().set(Main.NO_DROP, PersistentDataType.BYTE, (byte) 1);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    // Save own tags
    public ItemBuilder setTagData(String value) {
        meta.getPersistentDataContainer().set(Main.TAG_KEY, PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder setCustomData(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    // Return the Finished Item
    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }


    public static String getTagData(ItemStack item) {
        if(item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(Main.TAG_KEY, PersistentDataType.STRING);
    }

    public static boolean hasTagData(ItemStack item) {
        if(item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Main.TAG_KEY, PersistentDataType.STRING);
    }

    public static boolean hasPersistedDataContainer(ItemStack item) {
        if(item == null || item.getItemMeta() == null) return false;
        return !item.getItemMeta().getPersistentDataContainer().isEmpty();
    }

    public static boolean isUnmovable(ItemStack item) {
        if(item == null || item.getItemMeta() == null) return false;
        Byte tag = item.getItemMeta().getPersistentDataContainer().get(Main.NO_MOVE, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }
}
