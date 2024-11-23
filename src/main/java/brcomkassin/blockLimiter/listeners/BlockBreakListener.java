package brcomkassin.blockLimiter.listeners;

import brcomkassin.blockLimiter.limiter.BlockLimiter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.sql.SQLException;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) throws SQLException {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        Location location = event.getBlock().getLocation();

        BlockLimiter.recordBlockBreak(player, material, location);
    }
}


