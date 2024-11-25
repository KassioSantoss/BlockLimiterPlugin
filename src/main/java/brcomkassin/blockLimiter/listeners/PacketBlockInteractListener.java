package brcomkassin.blockLimiter.listeners;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;
import brcomkassin.utils.Message;

public class PacketBlockInteractListener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    public static void register(Plugin plugin) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    Player player = event.getPlayer();
                    PacketContainer packet = event.getPacket();
                    
                    Object hitResult = packet.getModifier().read(0);
                    if (hitResult == null) return;

                    java.lang.reflect.Method getBlockPos = hitResult.getClass().getMethod("a");
                    Object blockPosNMS = getBlockPos.invoke(hitResult);
                    if (blockPosNMS == null) return;

                    java.lang.reflect.Method getX = blockPosNMS.getClass().getMethod("u");
                    java.lang.reflect.Method getY = blockPosNMS.getClass().getMethod("v");
                    java.lang.reflect.Method getZ = blockPosNMS.getClass().getMethod("w");

                    int x = (int) getX.invoke(blockPosNMS);
                    int y = (int) getY.invoke(blockPosNMS);
                    int z = (int) getZ.invoke(blockPosNMS);

                    Location location = new Location(player.getWorld(), x, y, z);
                    Block block = location.getBlock();

                    if (block.getType() == Material.AIR) return;

                    boolean isCurrentBlockLimited = BlockLimiter.isLimitedBlock(block.getType());
                    Material originalType = block.getType();
                    Location finalLocation = location;

                    if (!isCurrentBlockLimited) {
                        Block nearestLimitedBlock = null;
                        double nearestDistance = Double.MAX_VALUE;

                        for (int dx = -3; dx <= 3; dx++) {
                            for (int dy = -3; dy <= 3; dy++) {
                                for (int dz = -3; dz <= 3; dz++) {
                                    Location nearbyLoc = location.clone().add(dx, dy, dz);
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

                    String itemId = player.getInventory().getItemInMainHand().getType().name();
                    if (ConfigManager.isBlockedItem(itemId)) {
                        Message.Chat.send(player, ConfigManager.getMessage("blocked-interaction"));
                        event.setCancelled(true);
                        return;
                    }

                    AtomicBoolean processed = new AtomicBoolean(false);
                    final Material finalType = originalType;
                    final Location finalLoc = finalLocation;

                    for (int delay : new int[]{1, 2}) {
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try {
                                if (processed.get()) return;

                                Material currentType = finalLoc.getBlock().getType();
                                if (currentType == Material.AIR) {
                                    if (processed.compareAndSet(false, true)) {
                                        BlockLimiter.recordBlockBreak(player, finalType, finalLoc);
                                    }
                                }
                            } catch (SQLException e) {
                                LOGGER.log(Level.SEVERE, 
                                    "Erro ao processar verificação do bloco para o jogador " + player.getName(), e);
                            }
                        }, delay);
                    }

                } catch (FieldAccessException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar pacote USE_ITEM", e);
                }
            }
        });
    }
} 