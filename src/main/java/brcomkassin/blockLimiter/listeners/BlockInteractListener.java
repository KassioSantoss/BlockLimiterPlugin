package brcomkassin.blockLimiter.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import brcomkassin.blockLimiter.ConstructionWandItems;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.utils.Message;
import lombok.SneakyThrows;

public class BlockInteractListener implements Listener {

    @SneakyThrows
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || block.getType() == Material.AIR) return;

        String itemId = player.getInventory().getItemInMainHand().getType().name();

        if (!BlockLimiter.isLimitedBlock(block.getType())) return;

        if (!ConstructionWandItems.isWandItem(itemId)) return;
        Message.Chat.send(player, "&4Você não pode usar esse item em um bloco limitado");
        event.setCancelled(true);
    }

}
