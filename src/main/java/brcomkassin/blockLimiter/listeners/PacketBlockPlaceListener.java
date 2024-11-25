package brcomkassin.blockLimiter.listeners;

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

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;
import brcomkassin.utils.Message;

public class PacketBlockPlaceListener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    public static void register(Plugin plugin) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    Player player = event.getPlayer();
                    
                    if (player.hasPermission("limites.bypass")) return;

                    PacketContainer packet = event.getPacket();
                    Object hitResult = packet.getModifier().read(0);
                    if (hitResult == null) return;

                    java.lang.reflect.Method getBlockPos = hitResult.getClass().getMethod("a");
                    Object blockPosNMS = getBlockPos.invoke(hitResult);
                    if (blockPosNMS == null) return;

                    java.lang.reflect.Method getDirection = hitResult.getClass().getMethod("b");
                    Object direction = getDirection.invoke(hitResult);
                    if (direction == null) return;

                    java.lang.reflect.Method getX = blockPosNMS.getClass().getMethod("u");
                    java.lang.reflect.Method getY = blockPosNMS.getClass().getMethod("v");
                    java.lang.reflect.Method getZ = blockPosNMS.getClass().getMethod("w");

                    int x = (int) getX.invoke(blockPosNMS);
                    int y = (int) getY.invoke(blockPosNMS);
                    int z = (int) getZ.invoke(blockPosNMS);

                    switch(direction.toString().toLowerCase()) {
                        case "up": y++; break;
                        case "down": y--; break;
                        case "north": z--; break;
                        case "south": z++; break;
                        case "west": x--; break;
                        case "east": x++; break;
                    }

                    Material material = player.getInventory().getItemInMainHand().getType();
                    Location location = new Location(player.getWorld(), x, y, z);

                    BlockGroup group = BlockLimiter.findGroupForMaterial(material);
                    if (group == null) return;

                    if (!BlockLimiter.canPlaceBlock(player, material, location)) {
                        Message.Chat.send(player, 
                            ConfigManager.getMessage("limit-reached"),
                            ConfigManager.getMessage("check-limits"),
                            ConfigManager.getMessage("invalid-blocks-note")
                        );
                        event.setCancelled(true);
                        return;
                    }

                    AtomicBoolean processed = new AtomicBoolean(false);

                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            if (processed.get()) return;

                            Block block = location.getBlock();
                            if (block.getType() == material) {
                                BlockLimiter.recordBlockPlacement(player, material, location);
                                processed.set(true); 
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, 
                                "Erro ao registrar colocação do bloco para o jogador " + player.getName(), e);
                        }
                    }, 2L);

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar pacote de colocação de bloco", e);
                }
            }
        });
    }
} 