package de.dachente.sbm.managers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.sign.Side;
import org.bukkit.util.BoundingBox;

import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.enums.Gate;
import de.dachente.sbm.utils.enums.SignFrame;
import de.dachente.sbm.utils.enums.Team;

public class GateManager {

    public static EnumMap<Team, Boolean> teamGateActive = new EnumMap<>(Team.class);
    private static Map<Gate, List<Location>> barrierPerGate = new HashMap<>();
    private static Map<Gate, List<Location>> barrierBox = new HashMap<>();

    public static List<Location> getPresurePlatesPos() {
       List<Location> pressurePlates = new ArrayList<>();
       for(Team team : Team.values()) pressurePlates.addAll(getPresurePlatesPos(team));
       return pressurePlates; 
    }

    public static List<Location> getPresurePlatesPos(Team team) {
        List<Location> plates = new ArrayList<>();
        List<Gate> gates = Gate.getGateByTeam(team);
        for(Entry<Location, Gate> entry : pressurePlates.entrySet()) if(gates.contains(entry.getValue())) plates.add(entry.getKey());
        return plates;
    }

    public static Gate getGate(Location plate) {
        return pressurePlates.get(plate);
    }

    public static Team getTeamByPresurePlatePos(Location pos) {
        Team team = null;
        for(Team currentTeam : Team.values()) {
            if(!getPresurePlatesPos(currentTeam).contains(pos)) continue;
            team = currentTeam;
            break;
        }
        return team;
    }

    private static Map<Location, Gate> pressurePlates = new HashMap<>();

    public static void setPressurePlates(Map<Location, Gate> pressurePlates) {
        GateManager.pressurePlates = pressurePlates;
    }

    public static void setGateActive(boolean active) {
        for(Team team : Team.values()) setGateActive(active, team);
    }

    public static void setGateActive(boolean active, Team team) {
        teamGateActive.put(team, active);
        updateSigns();
    }

    public static boolean getGateActive(Team team) {
        return teamGateActive.getOrDefault(team, false);
    }

    public static void setBarriers(Gate gate, boolean open) {
        List<Location> barriers = getBarriersPerGate(gate);
        Material material = open ? Material.AIR : Material.BARRIER;
        Material replace = open ? Material.BARRIER : Material.AIR;
        Game.replaceBocks(barriers.get(0), barriers.get(1), replace, material);
    }

    public static List<Location> getPartnerPlates(Gate gate) {
        List<Location> plates = new ArrayList<>();
        for(Entry<Location, Gate> entry : pressurePlates.entrySet()) if(entry.getValue().equals(gate)) plates.add(entry.getKey()); 
        return plates;
    }

    public static boolean arePartnerPlatesStillOn(Gate gate) {
        for(Location currentPlate : getPartnerPlates(gate)) 
            if((currentPlate.getBlock().getBlockData() instanceof Powerable plateData) && plateData.isPowered()) return true;
        return false;
    }

    public static Gate getGateForBarrierBox(Location loc, Team team) {
        for(Gate gate : Gate.getGateByTeam(team)) if(containsBarrierBoxLoc(gate, loc)) return gate;            
        return null;
    }

    public static boolean containsBarrierBoxLoc(Gate gate, Location loc) {
        List<Location> barrierBoxLocations = barrierBox.get(gate);
        BoundingBox zone = BoundingBox.of(barrierBoxLocations.get(0).getBlock(), barrierBoxLocations.get(1).getBlock());
        return zone.contains(loc.getBlock().getLocation().toVector());
    }

    public static void setBarrierBox(Map<Gate, List<Location>> barrierBox) {
        GateManager.barrierBox = barrierBox;
    }

    public static List<Location> getBarriersPerGate(Gate gate) {
        return barrierPerGate.get(gate);
    }

    public static void setBarrierPerGate(Map<Gate, List<Location>> barrierPerGate) {
        GateManager.barrierPerGate = barrierPerGate;
    }


    //Sign Manager

    private static List<Location> blueSigns = new ArrayList<>();
    private static List<Location> redSigns = new ArrayList<>();

    public static void setSigns(List<Location> blue, List<Location> red) {
        blueSigns = blue;
        redSigns = red;
        updateSigns();
    }

    public static List<Location> getSigns() {
        List<Location> signs = new ArrayList<>();
        for(Team team : Team.values()) signs.addAll(getSigns(team));
        return signs;
    }

    public static List<Location> getSigns(Team team) {
        if(team == Team.RED) return redSigns;
        if(team == Team.BLUE) return blueSigns;
        return null;
    }

    public static void updateSigns() {
        for(Team team : Team.values()) updateSigns(team);
    }

    public static void updateSigns(Team team) {
        for(Location signLoc : getSigns(team)) {
            Sign freshSign = (Sign) signLoc.getBlock().getState();
            
            SignFrame unlocked = SignFrame.of(DyeColor.GREEN, true, "---------", "|  |  |  |  |", "|  |  |  |  |", "V V V V V");
            SignFrame locked = SignFrame.of(DyeColor.RED, true, "---------", "\\/", "/\\", "---------");

            SignFrame currentFrame = getGateActive(team) ? unlocked : locked;

            SignBuilder.loadFrameToSign(freshSign, Side.FRONT, currentFrame);
        }
    }

    //Idea to put a animation here

}
