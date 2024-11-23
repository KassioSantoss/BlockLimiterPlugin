package brcomkassin.blockLimiter.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import brcomkassin.blockLimiter.limiter.BlockLimiter;

public class PistonListener implements Listener {

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (BlockLimiter.isLimitedBlock(block.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (BlockLimiter.isLimitedBlock(block.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }
} 