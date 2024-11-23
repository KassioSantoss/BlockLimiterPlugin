package brcomkassin.blockLimiter.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;
import brcomkassin.utils.Message;
import lombok.SneakyThrows;

public class BlockInteractListener implements Listener {

    @SneakyThrows
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || block.getType() == Material.AIR) return;

        if (!BlockLimiter.isLimitedBlock(block.getType())) return;

        String itemId = player.getInventory().getItemInMainHand().getType().name();
        if (ConfigManager.isBlockedItem(itemId)) {
            Message.Chat.send(player, ConfigManager.getMessage("blocked-interaction"));
            event.setCancelled(true);
        }
    }

}
