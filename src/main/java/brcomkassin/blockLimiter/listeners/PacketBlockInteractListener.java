package brcomkassin.blockLimiter.listeners;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;

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
                    if (!BlockLimiter.isLimitedBlock(block.getType())) return;

                    String itemId = player.getInventory().getItemInMainHand().getType().name();
                    if (ConfigManager.isBlockedItem(itemId)) {
                        Message.Chat.send(player, ConfigManager.getMessage("blocked-interaction"));
                        event.setCancelled(true);
                        return;
                    }

                    scheduleBlockChecks(block, player, location);

                } catch (FieldAccessException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar pacote USE_ITEM", e);
                }
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    PlayerDigType digType = packet.getPlayerDigTypes().read(0);
                    
                    if (digType != PlayerDigType.START_DESTROY_BLOCK && 
                        digType != PlayerDigType.ABORT_DESTROY_BLOCK && 
                        digType != PlayerDigType.STOP_DESTROY_BLOCK) return;

                    Player player = event.getPlayer();
                    BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
                    Location location = blockPosition.toLocation(player.getWorld());
                    Block block = location.getBlock();

                    if (block.getType() == Material.AIR) return;
                    if (!BlockLimiter.isLimitedBlock(block.getType())) return;

                    scheduleBlockChecks(block, player, location);
                } catch (FieldAccessException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar pacote BLOCK_DIG", e);
                }
            }
        });
    }

    private static void scheduleBlockChecks(Block block, Player player, Location blockLocation) {
        AtomicBoolean processed = new AtomicBoolean(false);
        Material originalType = block.getType();

        for (int delay : new int[]{2, 6, 10}) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(BlockLimiterPlugin.getInstance(), () -> {
                try {
                    if (processed.get()) return;

                    Material currentType = block.getType();
                    if (currentType == Material.AIR) {
                        BlockLimiter.recordBlockBreak(player, originalType, blockLocation);
                        processed.set(true);
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, 
                        "Erro ao processar verificação do bloco para o jogador " + player.getName(), e);
                }
            }, delay);
        }
    }
} 