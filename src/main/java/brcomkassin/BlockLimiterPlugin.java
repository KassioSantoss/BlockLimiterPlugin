package brcomkassin;

import brcomkassin.blockLimiter.BlockLimiterCommand;
import brcomkassin.blockLimiter.BlockPlaceListener;
import brcomkassin.utils.SQLiteManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        SQLiteManager.connect();
        SQLiteManager.createTables();

        getCommand("limiter").setExecutor(new BlockLimiterCommand());
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);
    }

    @Override
    public void onDisable() {
        SQLiteManager.disconnect();
    }

    public static BlockLimiterPlugin getInstance() {
        return BlockLimiterPlugin.getPlugin(BlockLimiterPlugin.class);
    }

}
