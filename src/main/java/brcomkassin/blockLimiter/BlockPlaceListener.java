package brcomkassin.blockLimiter;

import brcomkassin.utils.Message;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        Player player = event.getPlayer();

        if (!BlockLimiter.isLimitedBlock(itemStack.getType().name())) return;

        if (BlockLimiter.reachedTheLimit(player, itemStack)) {
            Message.Chat.send(player, "&4&lVocê já atingiu o limite para este bloco: &4" + itemStack.getType().name());
            event.setCancelled(true);
            return;
        }

        BlockLimiter.incrementBlockCount(player, itemStack);
    }

}
