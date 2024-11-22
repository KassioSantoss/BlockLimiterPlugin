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
import java.util.UUID;

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

                addItemInInventory(itemId, playerCount, limit);
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
            int playerCount = getPlayerBlockCount(player.getUniqueId(), itemId);
            int limit = BlockLimiter.getBlockLimit(itemId);

            ItemStack personalizedItem = ItemBuilder.of(item)
                    .setLore("&c&lBlocos posicionados: &a" + playerCount + " &f/ &4" + limit)
                    .build();

            personalizedInventory.setItem(i, personalizedItem);
        }
        player.openInventory(personalizedInventory);
    }

    private static int getPlayerBlockCount(UUID playerUuid, String itemId) {
        String query = "SELECT count FROM block_count WHERE player_uuid = ? AND item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
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

    public static void addItemInInventory(String itemId, int playerCount, int limit) {
        Material item = Material.getMaterial(itemId);

        if (item == null) {
            throw new RuntimeException("[BlockLimiterPlugin]: Ocorreu um problema ao adicionar um item ao inventário, pois o item é nulo");
        }

        ItemStack build = ItemBuilder.of(item)
                .setLore("&c&lBlocos posicionados: &a" + playerCount + " &f/ &4" + limit)
                .build();
        build.setAmount(1);
        INVENTORY.addItem(build);
    }

    public static void removeItemInInventory(ItemStack itemToRemove) {
        for (int i = 0; i < INVENTORY.getSize(); i++) {
            ItemStack item = INVENTORY.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            if (item.getType().name().equalsIgnoreCase(itemToRemove.getType().name())) {
                INVENTORY.clear(i);
            }
        }
        reorganizeInventory();
    }
}
