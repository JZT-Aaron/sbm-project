package de.dachente.sbm.utils.enums;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import de.dachente.sbm.utils.ItemBuilder;

public enum Language {
    DE("de", Locale.GERMAN, "Deutsch", "http://textures.minecraft.net/texture/5e7899b4806858697e283f084d9173fe487886453774626b24bd8cfecc77b3f", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ=="),
    EN("en", Locale.ENGLISH, "English", "http://textures.minecraft.net/texture/879d99d9c46474e2713a7e84a95e4ce7e8ff8ea4d164413a592e4435d2c6f9dc", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZDk5ZDljNDY0NzRlMjcxM2E3ZTg0YTk1ZTRjZTdlOGZmOGVhNGQxNjQ0MTNhNTkyZTQ0MzVkMmM2ZjlkYyJ9fX0=");

    private String fileName;
    private FileConfiguration file;
    private Locale locale;
    private String displayName;
    private String scullUrl;
    private String scullTexture;

    private Language(String fileName, Locale locale, String displayName, String scullUrl, String scullTexture) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.locale = locale;
        this.scullUrl = scullUrl;
        this.scullTexture = scullTexture;
    }

    public void setFile(FileConfiguration file) {
        this.file = file;
    }

    public ItemStack getScull() {
        return new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§f§l" + displayName).setSkullTexture(scullTexture, scullUrl)
        .setTagData("select-lang-" + this.toString()).setUnmovable().build();
    }

    public Locale getLocale() {
        return locale;
    }

    public FileConfiguration getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }
}
