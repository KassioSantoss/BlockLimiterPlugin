package brcomkassin;

import brcomkassin.blockLimiter.commands.BlockLimiterCommand;
import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.listeners.BlockBreakListener;
import brcomkassin.blockLimiter.listeners.BlockInteractListener;
import brcomkassin.blockLimiter.listeners.BlockPlaceListener;
import brcomkassin.blockLimiter.listeners.InventoryClickListener;
import brcomkassin.database.SQLiteManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        SQLiteManager.connectAndCreateTables();
        LimiterInventory.initializeInventory();

        getCommand("limites").setExecutor(new BlockLimiterCommand());
        registerListeners(new BlockPlaceListener(),
                new BlockInteractListener(),
                new BlockBreakListener(),
                new InventoryClickListener());
    }

    @Override
    public void onDisable() {
        SQLiteManager.disconnect();
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static BlockLimiterPlugin getInstance() {
        return BlockLimiterPlugin.getPlugin(BlockLimiterPlugin.class);
    }

}
