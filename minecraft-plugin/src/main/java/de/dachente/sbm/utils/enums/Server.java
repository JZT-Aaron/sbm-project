package de.dachente.sbm.utils.enums;

import org.bukkit.World;

public enum Server {
    LOBBY("SBM-Lobby", Status.INLOBBY),
    EVENT_SERVER("SBM-Arena", Status.WATCHING);

    private String name;
    private Status status;
    private World world;

    Server(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    public String getWorldName() {
        return name;
    }

    public void setWorld(World world) {
        this.world = world;
        
    }

    public World getWorld() {
        return world;
    }

    public Status getStatus() {
        return status;
    }
}
