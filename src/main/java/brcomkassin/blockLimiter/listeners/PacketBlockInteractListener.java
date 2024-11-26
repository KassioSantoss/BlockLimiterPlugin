package brcomkassin.blockLimiter.listeners;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.utils.BlockBreakUtil;
import brcomkassin.blockLimiter.utils.BlockBreakUtil.LimitedBlockInfo;
import brcomkassin.config.ConfigManager;
import brcomkassin.blockLimiter.utils.Message;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import lombok.SneakyThrows;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class PacketBlockInteractListener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    public static void register(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Client.USE_ITEM) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    try {
                        Location location = getLocationFromPacket(event);
                        if (location == null) return;

                        if (location.getBlock().getType() == Material.AIR) return;

                        LimitedBlockInfo blockInfo = BlockBreakUtil.findLimitedBlock(location.getBlock());
                        if (!blockInfo.isLimited()) return;

                        if (isBlockedItem(event)) {
                            Message.Chat.send(event.getPlayer(), ConfigManager.getMessage("blocked-interaction"));
                            event.setCancelled(true);
                            return;
                        }

                        BlockBreakUtil.scheduleBlockBreakCheck(
                            event.getPlayer(),
                            blockInfo.type(),
                            blockInfo.location()
                        );

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erro ao processar pacote USE_ITEM", e);
                    }
                }
            }
        );
    }

    @SneakyThrows
    private static Location getLocationFromPacket(PacketEvent event) {
        Object hitResult = event.getPacket().getModifier().read(0);
        if (hitResult == null) return null;

        Object blockPosNMS = hitResult.getClass().getMethod("a").invoke(hitResult);
        if (blockPosNMS == null) return null;

        int x = (int) blockPosNMS.getClass().getMethod("u").invoke(blockPosNMS);
        int y = (int) blockPosNMS.getClass().getMethod("v").invoke(blockPosNMS);
        int z = (int) blockPosNMS.getClass().getMethod("w").invoke(blockPosNMS);

        return new Location(event.getPlayer().getWorld(), x, y, z);
    }

    private static boolean isBlockedItem(PacketEvent event) {
        String itemId = event.getPlayer().getInventory().getItemInMainHand().getType().name();
        return ConfigManager.isBlockedItem(itemId);
    }
} 