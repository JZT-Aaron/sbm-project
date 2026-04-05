package de.dachente.sbm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.managers.DemoManger;
import de.dachente.sbm.managers.Info;

public class CommandListener implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();

        // When DEMO is TRUE
        if(Main.isDemo && message.startsWith("/gamerule")) {
            event.setCancelled(true);
            Info.sendLangError("no-permission", event.getPlayer());     
        }
        if(Main.isDemo && message.startsWith("/stop")) {
            event.setCancelled(true);
            DemoManger.closeServer();
            Info.sendInfo("Thanks for Testing the Demo.");
        }
    }

     
}