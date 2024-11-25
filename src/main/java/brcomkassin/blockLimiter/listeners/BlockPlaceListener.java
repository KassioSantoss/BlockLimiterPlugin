package brcomkassin.blockLimiter.listeners;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;

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

            AtomicBoolean processed = new AtomicBoolean(false);
            
            for (int delay : new int[]{2, 6, 10}) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                    try {
                        if (processed.get()) return;

                        if (location.getBlock().getType() == material) {
                            BlockLimiter.recordBlockPlacement(player, material, location);
                            processed.set(true);
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, 
                            "Erro ao registrar colocação do bloco para o jogador " + player.getName(), e);
                    }
                }, delay);
            }

        } catch (IllegalArgumentException | SQLException e) {
            LOGGER.log(Level.SEVERE, 
                "Erro ao processar colocação do bloco para o jogador " + player.getName(), e);
            event.setCancelled(true);
        }
    }
} 