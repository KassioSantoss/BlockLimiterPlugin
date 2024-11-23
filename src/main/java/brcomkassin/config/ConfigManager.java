package brcomkassin.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import brcomkassin.BlockLimiterPlugin;

public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    private static final Set<String> blockedItems = new HashSet<>();
    private static FileConfiguration config;

    public static void loadConfig() {
        BlockLimiterPlugin plugin = BlockLimiterPlugin.getInstance();
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Carrega os itens bloqueados
        blockedItems.clear();
        List<String> items = config.getStringList("blocked-items");
        blockedItems.addAll(items);
        
        LOGGER.log(Level.INFO, "Carregados {0} itens bloqueados da configura\u00e7\u00e3o", blockedItems.size());
    }

    public static boolean isBlockedItem(String itemId) {
        return blockedItems.contains(itemId.toLowerCase());
    }

    public static void reloadConfig() {
        BlockLimiterPlugin plugin = BlockLimiterPlugin.getInstance();
        plugin.reloadConfig();
        loadConfig();
    }
} 