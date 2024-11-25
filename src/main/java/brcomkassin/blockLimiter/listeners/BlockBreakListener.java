package brcomkassin.blockLimiter.listeners;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.utils.BlockBreakUtil;
import brcomkassin.blockLimiter.utils.BlockBreakUtil.LimitedBlockInfo;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.logging.Level;

public class BlockBreakListener implements Listener {
    private static final java.util.logging.Logger LOGGER = BlockLimiterPlugin.getInstance().getLogger();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            LimitedBlockInfo blockInfo = BlockBreakUtil.findLimitedBlock(event.getBlock());
            
            if (!blockInfo.isLimited()) return;
            
            BlockBreakUtil.scheduleBlockBreakCheck(
                event.getPlayer(),
                blockInfo.type(),
                blockInfo.location()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar quebra de bloco", e);
        }
    }
} 