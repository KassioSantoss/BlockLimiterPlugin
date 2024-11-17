package brcomkassin.blockLimiter.listeners;

import brcomkassin.blockLimiter.limiter.BlockLimiter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import java.sql.SQLException;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) throws SQLException {
        String blockId = event.getBlock().getType().name();
        Player player = event.getPlayer();

        if (!BlockLimiter.isLimitedBlock(blockId)) return;

        BlockLimiter.decrementBlockCount(player,blockId);
    }

}


