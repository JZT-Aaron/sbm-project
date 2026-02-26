package de.dachente.sbm.utils;

import de.dachente.sbm.main.Main;
import org.bukkit.Color;
import org.bukkit.Location;

public enum Team {

    BLUE("§9", Color.BLUE,"Team Blau", "team-blue", new Location(Main.arena, 71.5, 0, 45.5, 0, 0)),
    RED("§c", Color.RED,"Team Rot", "team-red", new Location(Main.arena, 71.5, 0, -44.5, 180, 0));


    Team(String chatColor, Color color, String name, String id, Location teamSpawnLocation) {
        this.chatColor = chatColor;
        this.color = color;
        this.name = name;
        this.id = id;
        this.teamSpawnLocation = teamSpawnLocation;
    }

    private String chatColor;
    private Color color;
    private String name;
    private String id;
    private Location teamSpawnLocation;

    public String getChatColor() {
        return chatColor;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public Location getTeamSpawnLocation() {
        return teamSpawnLocation;
    }

    public static Team getTeamById(String id) {
        for(Team team : Team.values()) {
            if(!team.getId().equalsIgnoreCase(id)) continue;;
            return team;
        }
        return null;
    }
}
