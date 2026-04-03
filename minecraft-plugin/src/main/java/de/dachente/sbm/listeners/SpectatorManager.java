package de.dachente.sbm.listeners;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.Game;
import de.dachente.sbm.utils.GameStat;
import de.dachente.sbm.utils.GameStats;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SpectatorManager implements Listener {

    public static void addSpectatorPlayers(Player player, String armorStand) {
        Map<String, String> spectatorPlayers = getSpectatorPlayers();
        spectatorPlayers.put(player.getUniqueId().toString(), armorStand);
        GameStats.set(GameStat.SPECTATOR_PLAYERS, spectatorPlayers);
    }

    public static void removeSpectatorPlayers(Player player) {
        Map<String, String> spectatorPlayers = getSpectatorPlayers();
        spectatorPlayers.remove(player.getUniqueId().toString());
        GameStats.set(GameStat.SPECTATOR_PLAYERS, spectatorPlayers);
        ArmorStand armorStand = getArmorStand(getSpectatorPlayers().get(player.getUniqueId().toString()));
        if(armorStand == null) {
            Main.getPlugin().getLogger().warning(player.getName() + "'s Arrmorstand has not been found.");
            return;
        }
        armorStand.remove();
        for(Player all : Bukkit.getOnlinePlayers()) all.showPlayer(Main.getPlugin(), player);
    }

    public static Map<String, String> getSpectatorPlayers() {
        return GameStats.get(GameStat.SPECTATOR_PLAYERS);
    }

    public static ArmorStand getArmorStand(String id) {
        for(ArmorStand armorStand : Main.arena.getEntitiesByClass(ArmorStand.class)) if(armorStand.getScoreboardTags().contains(id)) return armorStand;
        return null;
    }

    @EventHandler
    public void onSnake(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // When in Camera sneaking set Back as Viewer.
        if(!getSpectatorPlayers().containsKey(player.getUniqueId().toString())) return;
        removeSpectatorPlayers(player);
        Game.setViewer(player);
    }
}
