package brcomkassin.blockLimiter.listeners;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;
import brcomkassin.utils.Message;

public class BlockPlaceListener implements Listener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        Location location = event.getBlock().getLocation();

        if (player.hasPermission("limites.bypass")) return;

        BlockGroup group = BlockLimiter.findGroupForMaterial(material);
        if (group == null) return;

        try {
            if (!BlockLimiter.canPlaceBlock(player, material, location)) {
                event.setCancelled(true);
                Message.Chat.send(player, 
                    ConfigManager.getMessage("limit-reached"),
                    ConfigManager.getMessage("check-limits"),
                    ConfigManager.getMessage("invalid-blocks-note")
                );
                return;
            }

            BlockLimiter.recordBlockPlacement(player, material, location);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, 
                "Erro ao processar colocação do bloco para o jogador " + player.getName(), e);
            event.setCancelled(true);
        }
    }
} 