package de.dachente.sbm.utils.enums;

public enum PlayerStat {
    LANGUAGE("language"),
    PLAYERSTATUS("playerstatus"),
    SNOWING("snowing");

    private String id; 

    private PlayerStat(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
