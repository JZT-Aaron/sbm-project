package de.dachente.sbm.managers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.sign.Side;

import de.dachente.sbm.utils.Team;
import de.dachente.sbm.utils.enums.SignFrame;

public class GateManager {

    public static EnumMap<Team, Boolean> teamGateActive = new EnumMap<>(Team.class);
    private static Map<Location, List<Location>> barrierPerGate = new HashMap<>();

    public static List<Location> getPresurePlatesPos() {
       List<Location> pressurePlates = new ArrayList<>();
       for(Team team : Team.values()) pressurePlates.addAll(getPresurePlatesPos(team));
       return pressurePlates; 
    }

    public static List<Location> getPresurePlatesPos(Team team) { 
        if(team == Team.BLUE) return bluePressurePlatesPos;
        if(team == Team.RED) return redPressurePlatesPos;
        return null;
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

    private static List<Location> bluePressurePlatesPos = new ArrayList<>();
    private static List<Location> redPressurePlatesPos = new ArrayList<>();

    public static void setPressurePlates(List<Location> blue, List<Location> red) {
        bluePressurePlatesPos = blue;
        redPressurePlatesPos = red;
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

    public static void setBarriers(Location plate, boolean open) {
        List<Block> barriers = getBarriersPerGate(plate).stream().map(Location::getBlock).toList();
        Material material = open ? Material.AIR : Material.BARRIER;
        for(Block barrier : barriers) barrier.setType(material);
    }

    public static List<Location> getPartnerPlates(Location plate) {
        List<Location> plates = new ArrayList<>();
        List<Location> barriers = getBarriersPerGate(plate);
        for(Map.Entry<Location, List<Location>> entry : barrierPerGate.entrySet()) {
            if(!entry.getValue().equals(barriers) || entry.getKey().equals(plate)) continue;
            plates.add(entry.getKey());
        }
        return plates;
    }

    public static boolean arePartnerPlatesStillOn(Location plate) {
        boolean bool = false;
        for(Location currentPlate : getPartnerPlates(plate)) {
            if(!(currentPlate.getBlock().getBlockData() instanceof Powerable plateData)) continue;
            if(!plateData.isPowered()) continue;
            
            bool = true;
            break;
        }
        return bool;
    }

    public static List<Location> getBarriersPerGate(Location plate) {
        return barrierPerGate.get(plate);
    }

    public static void setBarrierPerGate(Map<Location, List<Location>> barrierPerGate) {
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
