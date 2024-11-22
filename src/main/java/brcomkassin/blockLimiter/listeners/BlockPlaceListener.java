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

        if (player.hasPermission("limites.bypass")) return;

        if (!BlockLimiter.isLimitedBlock(itemID)) return;

        if (BlockLimiter.reachedTheLimit(player, itemID)) {
            Message.Chat.send(player, "&f[&2&lDuende Magnata&f]: &eVocê atingiu o limite máximo definido para este bloco, digite &c/limites &epara conferir as restrições.");
            event.setCancelled(true);
            return;
        }

        int playerCount = BlockLimiter.getBlockCount(player, itemID);
        int limit = BlockLimiter.getBlockLimit(itemID);

        if (playerCount > limit) {
            BlockLimiter.punished(player, itemID);
            event.setCancelled(true);
            return;
        }

        BlockLimiter.incrementBlockCount(player, itemID);
    }

}
