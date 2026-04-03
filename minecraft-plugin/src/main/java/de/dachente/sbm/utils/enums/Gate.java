package de.dachente.sbm.utils.enums;

import java.util.ArrayList;
import java.util.List;

public enum Gate {
    BLUE_A("blue-a"),
    BLUE_B("blue-b"),
    RED_A("red-a"),
    RED_B("red-b");

    private String id;

    private Gate(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static Gate getGateByString(String id) {
        for(Gate gate : Gate.values()) if(gate.getId().equals(id)) return gate;
        return null;
    }

    public static List<Gate> getGateByTeam(Team team) {
        String id = team.getId().replace("team-", "");
        List<Gate> gates = new ArrayList<>();
        for(Gate gate : Gate.values()) if(gate.getId().contains(id)) gates.add(gate);
        return gates;
    }

    public static Gate getOppositColor(Gate gate) {
        String gateId = gate.getId();
        return getGateByString(gateId.contains("red") ? gateId.replace("red", "blue") : gateId.replace("blue", "red"));
    }

}
