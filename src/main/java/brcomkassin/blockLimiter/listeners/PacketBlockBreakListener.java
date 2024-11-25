package brcomkassin.blockLimiter.listeners;

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

public class PacketBlockBreakListener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    public static void register(Plugin plugin) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    PlayerDigType digType = packet.getPlayerDigTypes().read(0);
                    
                    Player player = event.getPlayer();
                    BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
                    Location location = blockPosition.toLocation(player.getWorld());
                    Block block = location.getBlock();

                    if (digType != PlayerDigType.START_DESTROY_BLOCK && 
                        digType != PlayerDigType.STOP_DESTROY_BLOCK) return;

                    if (block.getType() == Material.AIR) return;
                    if (!BlockLimiter.isLimitedBlock(block.getType())) return;

                    Material originalType = block.getType();
                    AtomicBoolean processed = new AtomicBoolean(false);

                    for (int delay : new int[]{2, 6, 10}) {
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try {
                                if (processed.get()) return;
                                Material currentType = block.getType();
                                if (currentType == Material.AIR) {
                                    BlockLimiter.recordBlockBreak(player, originalType, location);
                                    processed.set(true);
                                }
                            } catch (SQLException e) {
                                LOGGER.log(Level.SEVERE, 
                                    "Erro ao processar quebra do bloco para o jogador " + player.getName(), e);
                            }  
                        }, delay);
                    }
                } catch (FieldAccessException | IllegalArgumentException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar pacote de quebra de bloco", e);
                }
            }
        });
    }
} 