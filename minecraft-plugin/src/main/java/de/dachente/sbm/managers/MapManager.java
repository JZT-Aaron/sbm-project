package de.dachente.sbm.managers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.GameStat;
import de.dachente.sbm.utils.GameStats;
import de.dachente.sbm.utils.enums.GameMap;

public class MapManager {

    private static ConfigurationSection maps = Main.getPlugin().getConfig().getConfigurationSection("maps");

    public static final File MAPS_PATH = new File(Main.getPlugin().getDataFolder() + "/maps");

    public static void updateMapsNbtFiles() {
        for(GameMap map : GameMap.values()) {
            String id = map.getId();
            List<Location> positions =  Main.parseList(maps.getString(id));

            StructureManager manager = Bukkit.getStructureManager();
            Structure structure = manager.createStructure();

            structure.fill(positions.get(0), positions.get(1), true);

            if(!MAPS_PATH.exists()) MAPS_PATH.mkdirs();

            File file = new File(MAPS_PATH, id + ".nbt");
            try {
                manager.saveStructure(file, structure);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadMap(GameMap map) {
        StructureManager manager = Bukkit.getStructureManager();
        File file = new File(MAPS_PATH, map.getId() + ".nbt");
        if(!file.exists()) {
            updateMapsNbtFiles();
            if(!file.exists()) throw new IllegalArgumentException("This Map doesn't have a file");
        } 
        try {
            Structure structure = manager.loadStructure(file);
            Location pasteLocation = new Location(Main.arena, 54, -1, -22);
            GameStats.set(GameStat.LOADED_MAP, map);
            structure.place(pasteLocation, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
            Main.clearDroppedItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameMap getLoadedMap() {
        return GameStats.get(GameStat.LOADED_MAP);
    }

    public static Boolean isMapLoaded(GameMap map) {
        return getLoadedMap().equals(map);
    }
 
    public static void ifNotloadMap(GameMap map) {
        if(!getLoadedMap().equals(map)) loadMap(map);
    }
}
