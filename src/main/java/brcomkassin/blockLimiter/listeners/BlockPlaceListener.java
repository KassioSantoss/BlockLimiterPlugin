package brcomkassin.blockLimiter.listeners;

import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.utils.Message;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.sql.SQLException;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) throws SQLException {
        String itemID = event.getItemInHand().getType().name();
        Player player = event.getPlayer();

        if (!BlockLimiter.isLimitedBlock(itemID)) return;

        if (BlockLimiter.reachedTheLimit(player, itemID)) {
            Message.Chat.send(player, "&4&lVocê já atingiu o limite para este bloco: &4" + itemID);
            event.setCancelled(true);
            return;
        }

        BlockLimiter.incrementBlockCount(player, itemID);
    }

}
