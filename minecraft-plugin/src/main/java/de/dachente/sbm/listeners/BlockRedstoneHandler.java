package de.dachente.sbm.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import de.dachente.sbm.managers.GateManager;
import de.dachente.sbm.utils.Team;

public class BlockRedstoneHandler implements Listener {

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if(!(block.getType().name().contains("PRESSURE_PLATE") && GateManager.getPresurePlatesPos().contains(block.getLocation()))) return;
        Team team = GateManager.getTeamByPresurePlatePos(block.getLocation());
        if(!GateManager.getGateActive(team)) event.setNewCurrent(0);
        else {
            boolean open = event.getNewCurrent() > 0;
            if(!open && GateManager.arePartnerPlatesStillOn(block.getLocation())) return;
            GateManager.setBarriers(block.getLocation(), open);
        }
    }
}