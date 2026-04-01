package de.dachente.sbm.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import de.dachente.sbm.managers.LanguageManager;
import de.dachente.sbm.utils.PlayerStats;

public class AsyncPlayerPreLoginListener  implements Listener {

    @EventHandler
    public void onAsyncJoin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        //Load all Permanente Data before Player is in World
        if(!PlayerStats.containsPlayerSync(uuid)) PlayerStats.createPlayerSync(uuid, event.getName());
        else PlayerStats.loggedInSync(uuid);
        
        LanguageManager.addOnlineSnyc(uuid);
    }
    
}
