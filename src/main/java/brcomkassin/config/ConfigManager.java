package brcomkassin.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import brcomkassin.BlockLimiterPlugin;

public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    private static final Set<String> blockedItems = new HashSet<>();
    private static FileConfiguration config;

    public static void loadConfig() {
        BlockLimiterPlugin plugin = BlockLimiterPlugin.getInstance();
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        blockedItems.clear();
        List<String> items = config.getStringList("blocked-items");
        blockedItems.addAll(items.stream().map(String::toLowerCase).collect(Collectors.toList()));
        
        LOGGER.log(Level.INFO, "Carregados {0} itens bloqueados da configuração", blockedItems.size());
    }

    public static boolean isBlockedItem(String itemId) {
        return blockedItems.contains(itemId.toLowerCase());
    }

    public static String getMessage(String path) {
        String message = config.getString("messages." + path);
        if (message == null) {
            LOGGER.log(Level.WARNING, "Mensagem n\u00e3o encontrada: {0}", path);
            return "§cMensagem não configurada: " + path;
        }
        
        // Substitui o prefixo se existir
        String prefix = config.getString("messages.prefix", "");
        message = message.replace("%prefix%", prefix);
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        // Aplica substituições personalizadas (formato: %key%)
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        
        return message;
    }

    public static void reloadConfig() {
        BlockLimiterPlugin plugin = BlockLimiterPlugin.getInstance();
        plugin.reloadConfig();
        loadConfig();
    }
} 