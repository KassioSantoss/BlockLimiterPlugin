package brcomkassin.blockLimiter.limiter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.database.SQLiteManager;

public class BlockLimiter {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    private static final Map<String, BlockGroup> blockGroups = new HashMap<>();

    public static void addBlockGroup(String groupName, int limit, Material... materials) throws SQLException {
        if (getGroupIdByName(groupName) != null) {
            throw new SQLException("já existe um grupo com o nome " + groupName);
        }

        for (Material material : materials) {
            BlockGroup existingGroup = findGroupForMaterial(material);
            if (existingGroup != null) {
                throw new SQLException("O material " + material.name() + " já está no grupo " + existingGroup.getGroupName());
            }
        }

        String groupId = UUID.randomUUID().toString();
        BlockGroup group = new BlockGroup(groupId, groupName, limit);
        
        String query = "INSERT INTO block_limit (group_id, group_name, block_limit_value) VALUES (?, ?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, groupId);
            ps.setString(2, groupName);
            ps.setInt(3, limit);
            ps.executeUpdate();
        }

        blockGroups.put(groupId, group);

        for (Material material : materials) {
            String itemQuery = "INSERT INTO block_group_items (group_id, item_id) VALUES (?, ?)";
            try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(itemQuery)) {
                ps.setString(1, groupId);
                ps.setString(2, material.name());
                ps.executeUpdate();
                group.addMaterial(material);
            }
        }
        
        LimiterInventory.refreshInventory();
    }

    public static void addMaterialToGroup(String groupName, Material material) throws SQLException {
        BlockGroup existingGroup = findGroupForMaterial(material);
        if (existingGroup != null) {
            throw new SQLException("Este material já está no grupo " + existingGroup.getGroupName());
        }

        String groupId = getGroupIdByName(groupName);
        if (groupId == null) {
            throw new SQLException("Grupo não encontrado: " + groupName);
        }

        BlockGroup group = blockGroups.get(groupId);
        if (group == null) {
            throw new SQLException("Grupo não encontrado em memória: " + groupName);
        }

        String query = "INSERT INTO block_group_items (group_id, item_id) VALUES (?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, groupId);
            ps.setString(2, material.name());
            ps.executeUpdate();
        }
        group.addMaterial(material);
        LimiterInventory.refreshInventory();
    }

    public static boolean canPlaceBlock(Player player, Material material, Location location) throws SQLException {
        BlockGroup group = findGroupForMaterial(material);
        if (group == null) return true;

        int placedCount = getPlacedBlockCount(player.getUniqueId(), group.getGroupId());
        
        if (placedCount >= group.getLimit()) {
            boolean removedInvalid = verifyAndCleanPlacedBlocks(player, group.getGroupId());
            if (removedInvalid) {
                placedCount = getPlacedBlockCount(player.getUniqueId(), group.getGroupId());
            }
        }
        
        return placedCount < group.getLimit();
    }

    public static void recordBlockPlacement(Player player, Material material, Location location) throws SQLException {
        BlockGroup group = findGroupForMaterial(material);
        if (group == null) return;

        String query = "INSERT INTO placed_blocks (player_uuid, group_id, item_id, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, group.getGroupId());
            ps.setString(3, material.name());
            ps.setString(4, location.getWorld().getName());
            ps.setInt(5, location.getBlockX());
            ps.setInt(6, location.getBlockY());
            ps.setInt(7, location.getBlockZ());
            ps.executeUpdate();
        }

        recordBlockHistory(player, group.getGroupId(), material, location, "PLACE");
    }

    private static void recordBlockHistory(Player player, String groupId, Material material, Location location, String action) throws SQLException {
        String query = "INSERT INTO block_history (player_uuid, group_id, item_id, world, x, y, z, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, groupId);
            ps.setString(3, material.name());
            ps.setString(4, location.getWorld().getName());
            ps.setInt(5, location.getBlockX());
            ps.setInt(6, location.getBlockY());
            ps.setInt(7, location.getBlockZ());
            ps.setString(8, action);
            ps.executeUpdate();
        }
    }

    public static BlockGroup findGroupForMaterial(Material material) {
        if (material == null) return null;
        return blockGroups.values().stream()
                .filter(group -> group.containsMaterial(material))
                .findFirst()
                .orElse(null);
    }

    public static int getPlacedBlockCount(UUID playerUuid, String groupId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM placed_blocks WHERE player_uuid = ? AND group_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerUuid != null ? playerUuid.toString() : "%");
            ps.setString(2, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    public static void deleteGroup(String groupName) throws SQLException {
        String groupId = getGroupIdByName(groupName);
        if (groupId == null) {
            throw new SQLException("Grupo não encontrado: " + groupName);
        }

        String deleteItemsQuery = "DELETE FROM block_group_items WHERE group_id = ?";
        String deleteGroupQuery = "DELETE FROM block_limit WHERE group_id = ?";

        Connection conn = SQLiteManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps1 = conn.prepareStatement(deleteItemsQuery);
                 PreparedStatement ps2 = conn.prepareStatement(deleteGroupQuery)) {
                
                ps1.setString(1, groupId);
                ps1.executeUpdate();

                ps2.setString(1, groupId);
                ps2.executeUpdate();

                conn.commit();
                blockGroups.remove(groupId);
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        
        LimiterInventory.refreshInventory();
    }

    public static void updateGroupLimit(String groupName, int newLimit) throws SQLException {
        String groupId = getGroupIdByName(groupName);
        if (groupId == null) {
            throw new SQLException("Grupo não encontrado: " + groupName);
        }

        String query = "UPDATE block_limit SET block_limit_value = ? WHERE group_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setInt(1, newLimit);
            ps.setString(2, groupId);
            ps.executeUpdate();

            BlockGroup group = blockGroups.get(groupId);
            if (group != null) {
                BlockGroup updatedGroup = new BlockGroup(group.getGroupId(), group.getGroupName(), newLimit);
                group.getMaterials().forEach(updatedGroup::addMaterial);
                blockGroups.put(groupId, updatedGroup);
            }
        }
    }

    public static void removeMaterialFromGroup(String groupName, Material material) throws SQLException {
        String groupId = getGroupIdByName(groupName);
        if (groupId == null) {
            throw new SQLException("Grupo não encontrado: " + groupName);
        }

        BlockGroup group = blockGroups.get(groupId);
        if (group == null) {
            throw new SQLException("Grupo não encontrado em memória: " + groupName);
        }

        if (!group.containsMaterial(material)) {
            throw new SQLException("Este material não está no grupo " + groupName);
        }

        if (group.getMaterials().size() == 1 && group.getMaterials().contains(material)) {
            throw new SQLException("Não é possível remover o último item do grupo");
        }

        String deleteBlocksQuery = "DELETE FROM placed_blocks WHERE group_id = ? AND item_id = ?";
        String deleteItemQuery = "DELETE FROM block_group_items WHERE group_id = ? AND item_id = ?";

        Connection conn = SQLiteManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(deleteBlocksQuery)) {
                ps.setString(1, groupId);
                ps.setString(2, material.name());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteItemQuery)) {
                ps.setString(1, groupId);
                ps.setString(2, material.name());
                ps.executeUpdate();
            }

            group.getMaterials().remove(material);

            conn.commit();
            
            LimiterInventory.refreshInventory();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static String getGroupIdByName(String groupName) throws SQLException {
        String query = "SELECT group_id FROM block_limit WHERE group_name = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("group_id");
                }
            }
        }
        return null;
    }

    public static void recordBlockBreak(Player player, Material material, Location location) throws SQLException {
        BlockGroup group = findGroupForMaterial(material);
        if (group == null) return;

        String deleteQuery = "DELETE FROM placed_blocks WHERE player_uuid = ? AND world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(deleteQuery)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, location.getWorld().getName());
            ps.setInt(3, location.getBlockX());
            ps.setInt(4, location.getBlockY());
            ps.setInt(5, location.getBlockZ());
            ps.executeUpdate();
        }

        recordBlockHistory(player, group.getGroupId(), material, location, "BREAK");
    }

    public static int getBlockLimit(String groupId) throws SQLException {
        String query = "SELECT block_limit_value FROM block_limit WHERE group_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("block_limit_value");
                }
            }
        }
        return 0;
    }

    public static boolean isLimitedBlock(Material material) {
        return findGroupForMaterial(material) != null;
    }

    public static boolean isLimitedBlock(String materialName) {
        Material material = Material.getMaterial(materialName);
        return material != null && isLimitedBlock(material);
    }

    public static boolean verifyAndCleanPlacedBlocks(Player player, String groupId) throws SQLException {
        boolean removedAny = false;
        String query = "SELECT world, x, y, z, item_id FROM placed_blocks WHERE player_uuid = ? AND group_id = ?";
        
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, groupId);
            
            List<PlacedBlock> blocksToRemove = new ArrayList<>();
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String worldName = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String expectedItemId = rs.getString("item_id");
                    
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        blocksToRemove.add(new PlacedBlock(
                            player.getUniqueId(),
                            groupId,
                            Material.valueOf(expectedItemId),
                            new Location(null, x, y, z),
                            0
                        ));
                        continue;
                    }
                    
                    Location location = new Location(world, x, y, z);
                    Block block = world.getBlockAt(location);
                    Material actualMaterial = block.getType();
                    
                    if (!actualMaterial.name().equals(expectedItemId) || !findGroupForMaterial(actualMaterial).getGroupId().equals(groupId)) {
                        blocksToRemove.add(new PlacedBlock(
                            player.getUniqueId(),
                            groupId,
                            Material.valueOf(expectedItemId),
                            location,
                            0
                        ));
                    }
                }
            }
            
            if (!blocksToRemove.isEmpty()) {
                removedAny = true;
                String deleteQuery = "DELETE FROM placed_blocks WHERE player_uuid = ? AND group_id = ? AND world = ? AND x = ? AND y = ? AND z = ?";
                try (PreparedStatement deletePs = SQLiteManager.getConnection().prepareStatement(deleteQuery)) {
                    for (PlacedBlock block : blocksToRemove) {
                        deletePs.setString(1, block.getPlayerUuid().toString());
                        deletePs.setString(2, block.getGroupId());
                        deletePs.setString(3, block.getLocation().getWorld() != null ? block.getLocation().getWorld().getName() : "");
                        deletePs.setInt(4, block.getLocation().getBlockX());
                        deletePs.setInt(5, block.getLocation().getBlockY());
                        deletePs.setInt(6, block.getLocation().getBlockZ());
                        deletePs.executeUpdate();
                        
                        recordBlockHistory(player, groupId, block.getMaterial(), block.getLocation(), "REMOVED_INVALID");
                    }
                }
            }
        }
        
        return removedAny;
    }

    public static List<BlockGroup> getAllGroups() {
        return new ArrayList<>(blockGroups.values());
    }

    public static void loadGroupsFromDatabase() throws SQLException {
        blockGroups.clear(); 
        
        String groupQuery = "SELECT group_id, group_name, block_limit_value FROM block_limit";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(groupQuery)) {
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String groupId = rs.getString("group_id");
                String groupName = rs.getString("group_name");
                int limit = rs.getInt("block_limit_value");
                
                BlockGroup group = new BlockGroup(groupId, groupName, limit);
                blockGroups.put(groupId, group);
                
                String materialsQuery = "SELECT item_id FROM block_group_items WHERE group_id = ?";
                try (PreparedStatement ps2 = SQLiteManager.getConnection().prepareStatement(materialsQuery)) {
                    ps2.setString(1, groupId);
                    ResultSet rs2 = ps2.executeQuery();
                    
                    while (rs2.next()) {
                        String itemId = rs2.getString("item_id");
                        Material material = Material.getMaterial(itemId);
                        if (material != null) {
                            group.addMaterial(material);
                        }
                    }
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Carregados {0} grupos com suas configura\u00e7\u00f5es", blockGroups.size());
    }

    public static void removeBlockIfExists(Location location) {
        if (isBlockRegistered(location)) {
            removeBlockFromDatabase(location);
        }
    }

    public static boolean isBlockRegistered(Location location) {
        String query = "SELECT COUNT(*) as count FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar bloco registrado", e);
        }
        return false;
    }

    private static void removeBlockFromDatabase(Location location) {
        String query = "DELETE FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao remover bloco do banco de dados", e);
        }
    }

}
