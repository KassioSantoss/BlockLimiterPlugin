package brcomkassin.blockLimiter.inventory;

import brcomkassin.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LimiterInventory {

    private final static Inventory INVENTORY = Bukkit.createInventory(null, 54, LimiterInventoryType.NAME.getName());

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

    public static void addItemInInventory(ItemStack itemStack, int playerCount, int limit) {
        ItemStack build = ItemBuilder.of(itemStack)
                .setLore("&6Uso: &a" + playerCount + " &6| &4" + limit)
                .build();
        INVENTORY.addItem(build);
    }

}
