package brcomkassin.blockLimiter.utils;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class BlockBreakUtil {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    public static class LimitedBlockInfo {
        public final Material type;
        public final Location location;
        public final boolean isLimited;

        public LimitedBlockInfo(Material type, Location location, boolean isLimited) {
            this.type = type;
            this.location = location;
            this.isLimited = isLimited;
        }
    }

    public static LimitedBlockInfo findLimitedBlock(Block block) {
        Location location = block.getLocation();
        Material originalType = block.getType();
        boolean isLimited = BlockLimiter.isLimitedBlock(originalType);

        if (isLimited) {
            return new LimitedBlockInfo(originalType, location, true);
        }

        Block nearestLimitedBlock = findNearestLimitedBlock(location);
        if (nearestLimitedBlock != null) {
            return new LimitedBlockInfo(
                nearestLimitedBlock.getType(),
                nearestLimitedBlock.getLocation(),
                true
            );
        }

        return new LimitedBlockInfo(originalType, location, false);
    }

    private static Block findNearestLimitedBlock(Location location) {
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

        return nearestLimitedBlock;
    }

    public static void scheduleBlockBreakCheck(Player player, Material blockType, Location location) {
        AtomicBoolean processed = new AtomicBoolean(false);

        for (int delay : new int[]{1, 2}) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                try {
                    if (processed.get()) return;

                    if (location.getBlock().getType() == Material.AIR) {
                        if (processed.compareAndSet(false, true)) {
                            try {
                                BlockLimiter.recordBlockBreak(player, blockType, location);
                            } catch (SQLException e) {
                                handleSQLException(e, player, blockType, location);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.SEVERE, 
                        "Erro ao processar verificação do bloco para o jogador " + player.getName(), e);
                }
            }, delay);
        }
    }

    private static void handleSQLException(SQLException e, Player player, Material blockType, Location location) {
        if (e.getMessage().contains("SQLITE_BUSY")) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                try {
                    BlockLimiter.recordBlockBreak(player, blockType, location);
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