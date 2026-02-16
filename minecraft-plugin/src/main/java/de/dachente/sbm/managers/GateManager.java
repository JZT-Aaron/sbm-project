package de.dachente.sbm.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.Team;

public class GateManager {

    public static boolean isGateActive = false;
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

    public static void updateBarriers(Location loc) {

    }

    private static List<Location> bluePressurePlatesPos = new ArrayList<>();
    private static List<Location> redPressurePlatesPos = new ArrayList<>();

    public static void setBluePressurePlates(List<Location> blue, List<Location> red) {
        bluePressurePlatesPos = blue;
        redPressurePlatesPos = red;
    }

    public static List<Block> getGatesBarriers(Team team) {
        List<Block> gates = new ArrayList<>();
        if(team == Team.BLUE) {
            gates.add(Main.arena.getBlockAt(1, 1, 17));
            gates.add(Main.arena.getBlockAt(0, 1, 17));
            gates.add(Main.arena.getBlockAt(-1, 1, 17));
            gates.add(Main.arena.getBlockAt(0, 2, 17));
        }

        if(team == Team.RED) {
            gates.add(Main.arena.getBlockAt(1, 1, -17));
            gates.add(Main.arena.getBlockAt(0, 1, -17));
            gates.add(Main.arena.getBlockAt(-1, 1, -17));
            gates.add(Main.arena.getBlockAt(0, 2, -17));
        }
        return gates;
    }

    public static void setGateActive(boolean active) {
        isGateActive = active;
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
            
            if(plateData.isPowered()) {
                bool = true;
                break;
            }
        }
        return bool;
    }

    public static List<Location> getBarriersPerGate(Location plate) {
        return barrierPerGate.get(plate);
    }

    public static void setBarrierPerGate(Map<Location, List<Location>> barrierPerGate) {
        GateManager.barrierPerGate = barrierPerGate;
    }
}
