package brcomkassin;

import brcomkassin.blockLimiter.commands.BlockLimiterCommand;
import brcomkassin.blockLimiter.listeners.BlockBreakListener;
import brcomkassin.blockLimiter.listeners.BlockPlaceListener;
import brcomkassin.blockLimiter.listeners.InventoryClickListener;
import brcomkassin.database.SQLiteManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        SQLiteManager.connectAndCreateTables();

        getCommand("limiter").setExecutor(new BlockLimiterCommand());
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(),this);
    }

    @Override
    public void onDisable() {
        SQLiteManager.disconnect();
    }

    public static BlockLimiterPlugin getInstance() {
        return BlockLimiterPlugin.getPlugin(BlockLimiterPlugin.class);
    }

}
