package brcomkassin.utils;

import brcomkassin.BlockLimiterPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder() {
        this.itemStack = new ItemStack(Material.AIR);
    }

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(Material material, short data) {
        this.itemStack = new ItemStack(material, 1, data);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public static ItemBuilder of() {
        return new ItemBuilder();
    }

    public static ItemBuilder of(ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(Material material, short data) {
        return new ItemBuilder(material, data);
    }

    private ItemBuilder consumeMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        consumer.accept(meta);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder consume(Consumer<ItemStack> consumer) {
        consumer.accept(itemStack);
        return this;
    }

    private String translateColorCodes(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public ItemBuilder setName(String displayName) {
        return consumeMeta(meta -> meta.setDisplayName(translateColorCodes(displayName)));
    }

    public ItemBuilder setLore(List<String> lines) {
        return consumeMeta(meta -> meta.setLore(lines.stream().map(this::translateColorCodes).collect(Collectors.toList())));
    }

    public ItemBuilder setLore(String... lines) {
        return setLore(Arrays.asList(lines));
    }

    public ItemBuilder setAmount(int amount) {
        return consume(item -> item.setAmount(amount));
    }

    public ItemBuilder setType(Material material) {
        return consume(item -> item.setType(material));
    }

    @Deprecated
    public ItemBuilder setData(byte data) {
        return consume(item -> item.setData(new MaterialData(item.getType(), data)));
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        return consume(item -> item.addUnsafeEnchantment(enchantment, level));
    }

    public ItemBuilder setItemMetaData(String key) {
        return consumeMeta(meta -> {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(BlockLimiterPlugin.getInstance(), key);
            container.set(namespacedKey, PersistentDataType.STRING, key);
        });
    }

    public ItemBuilder setCustomModelData(int id) {
        return consumeMeta(meta -> meta.setCustomModelData(id));
    }

    public ItemBuilder getCustomModelData(int id) {
        return consumeMeta(ItemMeta::getCustomModelData);
    }

    public ItemBuilder setGlow(boolean mode) {
        return consumeMeta(meta -> {
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
    }

    public ItemStack build() {
        return this.itemStack;
    }
}