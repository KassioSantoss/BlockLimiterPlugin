package brcomkassin.blockLimiter.inventory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.blockLimiter.utils.InventoryItemBuilder;
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
    private static Inventory currentInventory = null;

    public static void initializeInventory() {
        try {
            clearInventory();
            setupBorder();
            loadGroups();
            setupNavigationButtons();
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
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, groups.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            BlockGroup group = groups.get(i);
            if (slot >= CONTENT_SLOTS.length) break;
            
            int actualSlot = CONTENT_SLOTS[slot++];
            AnimatedInventory.SLOT_GROUP_MAP.put(actualSlot, group);
            
            Material initialMaterial = group.getMaterials().iterator().next();
            int totalPlaced = getPlayerBlockCount(null, group.getGroupId());
            
            ItemStack item = InventoryItemBuilder.buildGroupItem(group, initialMaterial, null, totalPlaced);
            INVENTORY.setItem(actualSlot, item);
        }
    }

    public static void openInventory(Player player) throws SQLException {
        if (!isInitialized) {
            initializeInventory();
        }

        Inventory personalizedInventory = Bukkit.createInventory(null, INVENTORY.getSize(), 
            Component.text(InventoryType.LIMITER.getName()));
        
        currentInventory = personalizedInventory;
        
        setupBorder(personalizedInventory);
        loadGroupsForPlayer(player, personalizedInventory);
        setupNavigationButtons(personalizedInventory);
        
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
            int blockCount = BlockLimiter.getPlacedBlockCount(player.getUniqueId(), group.getGroupId());
            
            ItemStack item = InventoryItemBuilder.buildGroupItem(group, initialMaterial, player.getUniqueId(), blockCount);
            inventory.setItem(actualSlot, item);
        }
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

    public static void refreshInventory() {
        try {
            if (currentInventory != null) {
                for (int slot : CONTENT_SLOTS) {
                    currentInventory.setItem(slot, null);
                }
                
                setupBorder(currentInventory);
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getOpenInventory().getTopInventory().equals(currentInventory)) {
                        loadGroupsForPlayer(player, currentInventory);
                        setupNavigationButtons(currentInventory);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar inventário", e);
        }
    }

    private static void setupNavigationButtons(Inventory inventory) {
        List<BlockGroup> groups = BlockLimiter.getAllGroups();
        int totalPages = (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE);

        inventory.setItem(PREVIOUS_PAGE_SLOT, null);
        inventory.setItem(NEXT_PAGE_SLOT, null);

        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        if (currentPage > 0) {
            ItemStack previousPage = new ItemBuilder(Material.ARROW)
                    .setName("&aPágina Anterior")
                    .setLore("&7Clique para voltar para a página " + currentPage)
                    .build();
            inventory.setItem(PREVIOUS_PAGE_SLOT, previousPage);
        } else {
            inventory.setItem(PREVIOUS_PAGE_SLOT, borderItem);
        }

        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemBuilder(Material.ARROW)
                    .setName("&aPróxima Página")
                    .setLore("&7Clique para ir para a página " + (currentPage + 2))
                    .build();
            inventory.setItem(NEXT_PAGE_SLOT, nextPage);
        } else {
            inventory.setItem(NEXT_PAGE_SLOT, borderItem);
        }
    }

    private static void setupNavigationButtons() {
        setupNavigationButtons(INVENTORY);
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
}
