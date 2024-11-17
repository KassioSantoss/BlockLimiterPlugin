package brcomkassin.blockLimiter.inventory;

import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.database.SQLiteManager;
import brcomkassin.utils.ItemBuilder;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LimiterInventory {

    private final static Inventory INVENTORY = Bukkit.createInventory(null, 54, InventoryType.LIMITER.getName());
    private static boolean isInitialized = false;

    @SneakyThrows
    public static void initializeInventory() {
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement("SELECT item_id, block_limit_value FROM block_limit")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String itemId = rs.getString("item_id");
                int limit = rs.getInt("block_limit_value");

                Material material = Material.getMaterial(itemId);
                if (material == null) continue;

                ItemStack itemStack = new ItemStack(material);
                int playerCount = getBlockCount(itemId);

                addItemInInventory(itemStack, playerCount, limit);
            }
        }
        isInitialized = true;
    }

    private static int getBlockCount(String itemId) {
        String query = "SELECT count FROM block_count WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
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

    public static void reorganizeInventory() {
        List<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < INVENTORY.getSize(); i++) {
            ItemStack item = INVENTORY.getItem(i);
            if (item != null) {
                items.add(item);
                INVENTORY.clear(i);
            }
        }

        for (ItemStack item : items) {
            INVENTORY.addItem(item);
        }
    }

    public static void open(Player player) {
        player.openInventory(INVENTORY);
    }

    public static void openInventory(Player player) throws SQLException {
        if (!isInitialized) {
            initializeInventory();
        }

        Inventory personalizedInventory = Bukkit.createInventory(null, INVENTORY.getSize(), InventoryType.LIMITER.getName());
        personalizedInventory.clear();

        for (int i = 0; i < INVENTORY.getSize(); i++) {
            ItemStack item = INVENTORY.getItem(i);

            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = item.getType().name();
            int playerCount = getBlockCount(itemId);
            int limit = BlockLimiter.getBlockLimit(itemId);

            ItemStack personalizedItem = ItemBuilder.of(item)
                    .setLore("&6Uso: &a" + playerCount + " &6| &4" + limit)
                    .build();

            personalizedInventory.setItem(i, personalizedItem);
        }
        player.openInventory(personalizedInventory);
    }

    public static void addItemInInventory(ItemStack itemStack, int playerCount, int limit) {
        ItemStack build = ItemBuilder.of(itemStack)
                .setName("&f" + itemStack.getType().name())
                .setLore("&7Uso: &a" + playerCount + " &6| &4" + limit)
                .build();
        INVENTORY.addItem(build);
    }

    public static void removeItemInInventory(ItemStack itemToRemove) {
        INVENTORY.remove(itemToRemove);
        reorganizeInventory();
    }
}
