package de.dachente.sbm.utils.enums;

import org.bukkit.configuration.file.FileConfiguration;

public enum Language {
    DE("de"),
    EN("en");

    private String fileName;
    private FileConfiguration file;

    private Language(String fileName) {
        this.fileName = fileName;
    }

    public void setFile(FileConfiguration file) {
        this.file = file;
    }

    public FileConfiguration getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }
}
