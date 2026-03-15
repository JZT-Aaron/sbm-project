package de.dachente.sbm.utils.enums;

public enum GameMap {
    REMATCH("rematch"),
    GAME("game"),
    WINNER("winner");

    private String id; 

    private GameMap(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static GameMap fromId(String id) {
        for(GameMap map : GameMap.values()) {
            if(!map.getId().equals(id)) continue;
            return map;
        }
        return null;
    }
}
