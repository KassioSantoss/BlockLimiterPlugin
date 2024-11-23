package brcomkassin;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import brcomkassin.blockLimiter.commands.BlockLimiterCommand;
import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.listeners.BlockBreakListener;
import brcomkassin.blockLimiter.listeners.BlockInteractListener;
import brcomkassin.blockLimiter.listeners.BlockPlaceListener;
import brcomkassin.blockLimiter.listeners.InventoryClickListener;
import brcomkassin.blockLimiter.listeners.PistonListener;
import brcomkassin.database.SQLiteManager;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.config.ConfigManager;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigManager.loadConfig();
        
        SQLiteManager.connectAndCreateTables();
        try {
            BlockLimiter.loadGroupsFromDatabase();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Erro ao carregar grupos do banco de dados: {0}", e.getMessage());
        }
        registerCommand();
        LimiterInventory.initializeInventory();
        registerListeners(new BlockPlaceListener(),
                new BlockInteractListener(),
                new BlockBreakListener(),
                new InventoryClickListener(),
                new PistonListener());
    }

    @Override
    public void onDisable() {
        SQLiteManager.disconnect();
        LimiterInventory.cleanup();
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static BlockLimiterPlugin getInstance() {
        return BlockLimiterPlugin.getPlugin(BlockLimiterPlugin.class);
    }

    private void registerCommand() {
        PluginCommand command = getCommand("limites");
        if (command == null) {
            getLogger().warning("Comando 'limites' não encontrado no plugin.yml!");
            return;
        }
        
        BlockLimiterCommand executor = new BlockLimiterCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
