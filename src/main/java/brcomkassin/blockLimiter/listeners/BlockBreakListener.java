package brcomkassin.blockLimiter.listeners;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.comphenix.protocol.reflect.FieldAccessException;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockLimiter;

public class BlockBreakListener implements Listener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            Location location = block.getLocation();

            boolean isCurrentBlockLimited = BlockLimiter.isLimitedBlock(block.getType());
            Material originalType = block.getType();
            Location finalLocation = location;

            if (!isCurrentBlockLimited) {
                Block nearestLimitedBlock = null;
                double nearestDistance = Double.MAX_VALUE;

                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            Location nearbyLoc = location.clone().add(x, y, z);
                            Block nearbyBlock = nearbyLoc.getBlock();
                            if (BlockLimiter.isLimitedBlock(nearbyBlock.getType())) {
                                double distance = location.distanceSquared(nearbyLoc);
                                if (distance < nearestDistance) {
                                    nearestDistance = distance;
                                    nearestLimitedBlock = nearbyBlock;
                                }
                            }
                        }
                    }
                }

                if (nearestLimitedBlock != null) {
                    originalType = nearestLimitedBlock.getType();
                    finalLocation = nearestLimitedBlock.getLocation();
                    isCurrentBlockLimited = true;
                }
            }

            if (!isCurrentBlockLimited) return;

            AtomicBoolean processed = new AtomicBoolean(false);
            final Material finalType = originalType;
            final Location finalLoc = finalLocation;

            for (int delay : new int[]{1, 2}) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                    try {
                        if (processed.get()) return;

                        Material currentType = finalLoc.getBlock().getType();
                        if (currentType == Material.AIR) {
                            if (processed.compareAndSet(false, true)) {
                                try {
                                    BlockLimiter.recordBlockBreak(player, finalType, finalLoc);
                                } catch (SQLException e) {
                                    if (e.getMessage().contains("SQLITE_BUSY")) {
                                        org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                                            try {
                                                BlockLimiter.recordBlockBreak(player, finalType, finalLoc);
                                            } catch (SQLException ex) {
                                                LOGGER.log(Level.SEVERE, 
                                                    "Erro ao processar quebra do bloco após retry para o jogador " + 
                                                    player.getName(), ex);
                                            }
                                        }, 5L);
                                    } else {
                                        LOGGER.log(Level.SEVERE, 
                                            "Erro ao processar quebra do bloco para o jogador " + player.getName(), e);
                                    }
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.SEVERE, 
                            "Erro ao processar verificação do bloco para o jogador " + player.getName(), e);
                    }
                }, delay);
            }
        } catch (FieldAccessException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar quebra de bloco", e);
        }   
    }
} 