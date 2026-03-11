package de.dachente.sbm.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

import de.dachente.sbm.main.Main;

public class MutliLangSignManager {

    public static final Map<UUID, Map<Location, String[]>> customSigns = new HashMap<>();

    public static void registerListener() {
        Main.getPlugin().getLogger().info("MutliSign Listerner registers.");
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Main.getPlugin(), ListenerPriority.NORMAL, PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                Player player = event.getPlayer();

                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                Location loc = new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());

                if(customSigns.containsKey(player.getUniqueId()) && customSigns.get(player.getUniqueId()).containsKey(loc)) {
                    String[] lines = customSigns.get(player.getUniqueId()).get(loc);

                    NbtCompound nbt = (NbtCompound) packet.getNbtModifier().read(0);
                    if (!nbt.containsKey("front_text")) return;
                    
                    NbtCompound frontText = nbt.getCompound("front_text");
                    
                    List<String> jsonLines = new ArrayList<>();
                    for(String line : lines) {
                        jsonLines.add(line);
                    }
                    frontText.put("messages", NbtFactory.ofList("messages", jsonLines));
                    packet.getNbtModifier().write(0, nbt);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void sendSign(Player player, Location loc, String[] lines) {
        customSigns.computeIfAbsent(player.getUniqueId(), k-> new HashMap<>()).put(loc, lines);
        // Listener sets lines then
        
        player.sendSignChange(loc, lines);           
    }

    public static void sendLangSign(Player player, Location loc, String id) {

    }
    
}

