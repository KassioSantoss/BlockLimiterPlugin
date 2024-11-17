package brcomkassin.blockLimiter;

import brcomkassin.utils.ItemBuilder;
import brcomkassin.utils.Message;
import brcomkassin.utils.SQLiteManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class BlockLimiter {

    private final static Inventory INVENTORY = Bukkit.createInventory(null, 54, "Limites de Blocos");

    public static void addLimitedBlock(Player player, ItemStack itemStack, int limit) {
        String itemId = itemStack.getType().name();

        if (isLimitedBlock(itemId)) {
            Message.Chat.send(player, "&4O item já está limitado!");
            return;
        }

        saveBlockLimit(itemId, limit);
        int playerCount = getBlockCount(player.getUniqueId(), itemId);
        addItemInInventory(itemStack, playerCount, limit);
        Message.Chat.send(player, "&aO item foi limitado em &6" + limit + " &ausos!");
    }

    public static boolean isLimitedBlock(String itemId) {
        String query = "SELECT 1 FROM block_limit WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean reachedTheLimit(Player player,ItemStack itemStack){
        String itemId = itemStack.getType().name();
        UUID playerId = player.getUniqueId();
        int limit = getBlockLimit(itemId);
        int currentCount = getBlockCount(playerId, itemId);
        return currentCount >= limit;
    }

    public static void incrementBlockCount(Player player, ItemStack itemStack) {
        String itemId = itemStack.getType().name();

        if (!isLimitedBlock(itemId)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int limit = getBlockLimit(itemId);
        int currentCount = getBlockCount(playerId, itemId);

        saveBlockCount(playerId, itemId, currentCount + 1);
        int updatedCount = getBlockCount(playerId, itemId);
        updateInventory(itemId, updatedCount, limit);
    }

    private static void addItemInInventory(ItemStack itemStack, int playerCount, int limit) {
        ItemStack build = ItemBuilder.of(itemStack)
                .setLore("&6Uso: &a" + playerCount + " &6| &4" + limit)
                .build();
        INVENTORY.addItem(build);
    }

    public static void updateInventory(String itemId, int playerCount, int limit) {
        for (int i = 0; i < INVENTORY.getSize(); i++) {
            ItemStack item = INVENTORY.getItem(i);

            if (item == null || !item.getType().name().equals(itemId)) continue;

            ItemStack updatedItem = ItemBuilder.of(item)
                    .setLore("&6Uso: &a" + playerCount + " &6| &4" + limit)
                    .build();
            INVENTORY.setItem(i, updatedItem);
        }
    }

    public static void openInventory(Player player) {
        player.openInventory(INVENTORY);
    }

    private static void saveBlockLimit(String itemId, int limit) {
        String query = "INSERT OR REPLACE INTO block_limit (item_id, block_limit_value) VALUES (?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            ps.setInt(2, limit);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getBlockLimit(String itemId) {
        String query = "SELECT block_limit_value FROM block_limit WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("block_limit_value");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void saveBlockCount(UUID playerId, String itemId, int count) {
        String query = "INSERT OR REPLACE INTO block_count (player_uuid, item_id, count) VALUES (?, ?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setInt(3, count);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getBlockCount(UUID playerId, String itemId) {
        String query = "SELECT count FROM block_count WHERE player_uuid = ? AND item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
