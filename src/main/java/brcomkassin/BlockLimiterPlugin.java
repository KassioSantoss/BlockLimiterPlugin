package brcomkassin;

import brcomkassin.blockLimiter.commands.BlockLimiterCommand;
import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.listeners.BlockBreakListener;
import brcomkassin.blockLimiter.listeners.BlockPlaceListener;
import brcomkassin.blockLimiter.listeners.InventoryClickListener;
import brcomkassin.database.SQLiteManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        SQLiteManager.connectAndCreateTables();
        LimiterInventory.initializeInventory();

        getCommand("limiter").setExecutor(new BlockLimiterCommand());
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
    }

    @Override
    public void onDisable() {
        SQLiteManager.disconnect();
    }

    public static BlockLimiterPlugin getInstance() {
        return BlockLimiterPlugin.getPlugin(BlockLimiterPlugin.class);
    }

}
