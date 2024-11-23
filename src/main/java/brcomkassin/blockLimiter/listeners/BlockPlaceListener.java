package brcomkassin.blockLimiter.listeners;

import java.sql.SQLException;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;
import brcomkassin.utils.Message;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) throws SQLException {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        if (player.hasPermission("limites.bypass")) return;

        BlockGroup group = BlockLimiter.findGroupForMaterial(material);
        if (group == null) return;

        if (!BlockLimiter.canPlaceBlock(player, material, event.getBlock().getLocation())) {
            Message.Chat.send(player, 
                ConfigManager.getMessage("limit-reached"),
                ConfigManager.getMessage("check-limits"),
                ConfigManager.getMessage("invalid-blocks-note")
            );
            event.setCancelled(true);
            return;
        }

        BlockLimiter.recordBlockPlacement(player, material, event.getBlock().getLocation());
    }

}
