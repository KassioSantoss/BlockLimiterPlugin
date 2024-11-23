package brcomkassin.blockLimiter.inventory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;

import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.database.SQLiteManager;
import brcomkassin.utils.ItemBuilder;
import net.kyori.adventure.text.Component;

public class LimiterInventory {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    private final static Inventory INVENTORY = Bukkit.createInventory(null, 54, Component.text(InventoryType.LIMITER.getName()));
    private static boolean isInitialized = false;
    private static final int[] BORDER_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 17, 18, 26, 27, 35, 36, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int ITEMS_PER_PAGE = 28; 

    private static int currentPage = 0;

    public static void initializeInventory() {
        try {
            clearInventory();
            setupBorder();
            loadGroups();
            isInitialized = true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao inicializar inventário", e);
        }
    }

    private static void clearInventory() {
        INVENTORY.clear();
        AnimatedInventory.clearSlots();
    }

    private static void setupBorder() {
        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int slot : BORDER_SLOTS) {
            INVENTORY.setItem(slot, borderItem);
        }
    }

    private static void loadGroups() throws SQLException {
        List<BlockGroup> groups = BlockLimiter.getAllGroups();
        LOGGER.log(Level.INFO, "Carregando {0} grupos para o inventário", groups.size());
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, groups.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            BlockGroup group = groups.get(i);
            if (slot >= CONTENT_SLOTS.length) {
                LOGGER.warning("Limite de slots atingido, alguns grupos não serão exibidos");
                break;
            }
            
            int actualSlot = CONTENT_SLOTS[slot++];
            LOGGER.log(Level.INFO, "Registrando grupo {0} no slot {1}", new Object[]{group.getGroupName(), actualSlot});
            AnimatedInventory.SLOT_GROUP_MAP.put(actualSlot, group);
            updateSlot(actualSlot, null);
        }
    }

    public static void updateSlot(int slot, Player player) {
        Material currentMaterial;
        if (player != null) {
            currentMaterial = AnimatedInventory.getCurrentMaterial(slot, player.getUniqueId());
        } else {
            BlockGroup group = null;
            for (BlockGroup g : BlockLimiter.getAllGroups()) {
                if (!g.getMaterials().isEmpty()) {
                    group = g;
                    break;
                }
            }
            if (group == null) return;
            currentMaterial = group.getMaterials().iterator().next();
        }

        if (currentMaterial == null) {
            LOGGER.log(Level.WARNING, "Material atual é nulo para o slot {0}", slot);
            return;
        }

        BlockGroup group = BlockLimiter.findGroupForMaterial(currentMaterial);
        if (group == null) {
            LOGGER.log(Level.WARNING, "Grupo não encontrado para o material {0} no slot {1}", 
                new Object[]{currentMaterial, slot});
            return;
        }

        String groupName = capitalizeGroupName(group.getGroupName());
        List<String> lore = new ArrayList<>();
        
        try {
            UUID playerUuid = player != null ? player.getUniqueId() : null;
            int totalPlaced = getPlayerBlockCount(playerUuid, group.getGroupId());
            lore.add("&7Blocos no chão: &a" + totalPlaced + " &7/ &c" + group.getLimit());
            lore.add("");
            lore.add("&7Blocos neste grupo:");
            
            group.getMaterials().stream()
                    .map(material -> "&8- &7" + formatMaterialName(material.name()))
                    .forEach(lore::add);
                    
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar slot", e);
            return;
        }

        ItemStack item = new ItemBuilder(currentMaterial)
                .setName("&6" + groupName)
                .setLore(lore)
                .build();

        if (player != null) {
            player.getOpenInventory().setItem(slot, item);
        } else {
            INVENTORY.setItem(slot, item);
        }
    }

    private static String capitalizeGroupName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String formatMaterialName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static void openInventory(Player player) throws SQLException {
        if (!isInitialized) {
            initializeInventory();
        }

        Inventory personalizedInventory = Bukkit.createInventory(null, INVENTORY.getSize(), 
            Component.text(InventoryType.LIMITER.getName()));
        
        setupBorder(personalizedInventory);
        loadGroupsForPlayer(player, personalizedInventory);
        
        player.openInventory(personalizedInventory);
        AnimatedInventory.startAnimation(player);
    }

    private static void loadGroupsForPlayer(Player player, Inventory inventory) throws SQLException {
        List<BlockGroup> groups = BlockLimiter.getAllGroups();
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, groups.size());
        
        AnimatedInventory.clearSlots(); 
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            BlockGroup group = groups.get(i);
            if (slot >= CONTENT_SLOTS.length) break;
            
            int actualSlot = CONTENT_SLOTS[slot++];
            AnimatedInventory.registerSlot(actualSlot, group, player);
            
            Material initialMaterial = group.getMaterials().iterator().next();
            List<String> lore = new ArrayList<>();
            lore.add("&7Seus blocos: &a" + BlockLimiter.getPlacedBlockCount(player.getUniqueId(), group.getGroupId()) + " &7/ &c" + group.getLimit());
            lore.add("");
            lore.add("&7Blocos neste grupo:");
            group.getMaterials().forEach(material -> {
                String prefix = material.equals(initialMaterial) ? "&a" : "&7";
                lore.add("&8- " + prefix + formatMaterialName(material.name()));
            });

            ItemStack item = new ItemBuilder(initialMaterial)
                    .setName("&6" + formatGroupName(group.getGroupName()))
                    .setLore(lore)
                    .build();
            
            inventory.setItem(actualSlot, item);
        }

        AnimatedInventory.startAnimation(player);
        // AnimatedInventory.debugState(player);
    }

    private static void setupBorder(Inventory inventory) {
        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, borderItem);
        }
    }

    private static int getPlayerBlockCount(UUID playerUuid, String groupId) throws SQLException {
        String query;
        if (playerUuid == null) {
            query = "SELECT COUNT(*) as count FROM placed_blocks WHERE group_id = ?";
            try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
                ps.setString(1, groupId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                }
            }
        } else {
            query = "SELECT COUNT(*) as count FROM placed_blocks WHERE player_uuid = ? AND group_id = ?";
            try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, groupId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                }
            }
        }
        return 0;
    }

    public static void addItemInInventory(String itemId, int playerCount, int limit) {
        Material item = Material.getMaterial(itemId);

        if (item == null) {
            LOGGER.warning("[BlockLimiterPlugin]: Ocorreu um problema ao adicionar um item ao inventário, pois o item é nulo");
            return;
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
        reorderInventory();
    }

    private static void reorderInventory() {
        List<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < INVENTORY.getSize(); i++) {
            ItemStack item = INVENTORY.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
                INVENTORY.clear(i);
            }
        }

        for (ItemStack item : items) {
            INVENTORY.addItem(item);
        }
    }

    public static void refreshInventory() {
        try {
            clearInventory();
            setupBorder(INVENTORY);
            loadGroups();
            setupNavigationButtons();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar inventário", e);
        }
    }

    private static void setupNavigationButtons() {
        List<BlockGroup> groups = BlockLimiter.getAllGroups();
        int totalPages = (int) Math.ceil(groups.size() / (double) ITEMS_PER_PAGE);

        if (currentPage > 0) {
            ItemStack previousPage = new ItemBuilder(Material.ARROW)
                    .setName("&aPágina Anterior")
                    .setLore("&7Clique para voltar para a página " + currentPage)
                    .build();
            INVENTORY.setItem(PREVIOUS_PAGE_SLOT, previousPage);
        }

        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemBuilder(Material.ARROW)
                    .setName("&aPróxima Página")
                    .setLore("&7Clique para ir para a página " + (currentPage + 2))
                    .build();
            INVENTORY.setItem(NEXT_PAGE_SLOT, nextPage);
        }
    }

    public static void handleInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        if (event.getSlot() == PREVIOUS_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            refreshInventory();
            return;
        }
        
        if (event.getSlot() == NEXT_PAGE_SLOT) {
            List<BlockGroup> groups = BlockLimiter.getAllGroups();
            int totalPages = (int) Math.ceil(groups.size() / (double) ITEMS_PER_PAGE);
            
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshInventory();
            }
        }
    }

    private static String formatGroupName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
