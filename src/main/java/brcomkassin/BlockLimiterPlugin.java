package brcomkassin;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import brcomkassin.blockLimiter.commands.BlockLimiterCommand;
import brcomkassin.blockLimiter.inventory.InventoryType;
import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.blockLimiter.listeners.InventoryClickListener;
import brcomkassin.blockLimiter.listeners.InventoryCloseListener;
import brcomkassin.blockLimiter.listeners.PistonListener;
import brcomkassin.blockLimiter.listeners.PacketBlockInteractListener;
import brcomkassin.blockLimiter.listeners.PacketBlockBreakListener;
import brcomkassin.blockLimiter.listeners.BlockPlaceListener;
import brcomkassin.config.ConfigManager;
import brcomkassin.database.SQLiteManager;
import net.kyori.adventure.text.Component;

public final class BlockLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigManager.loadConfig();
        
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib não encontrado! O plugin não funcionará corretamente.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SQLiteManager.connectAndCreateTables();
        try {
            BlockLimiter.loadGroupsFromDatabase();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Erro ao carregar grupos do banco de dados: {0}", e.getMessage());
        }
        registerCommand();
        LimiterInventory.initializeInventory();

        registerSimpleListeners(
            new BlockPlaceListener(),
            new InventoryClickListener(),
            new InventoryCloseListener(),
            new PistonListener()
        );

        PacketBlockInteractListener.register(this);
        PacketBlockBreakListener.register(this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().title()
                    .equals(Component.text(InventoryType.LIMITER.getName()))) {
                player.closeInventory();
            }
        }
        SQLiteManager.disconnect();
    }

    private void registerSimpleListeners(Listener... listeners) {
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
