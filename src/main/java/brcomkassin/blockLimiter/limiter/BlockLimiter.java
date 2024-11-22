package brcomkassin.blockLimiter.limiter;

import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.utils.Message;
import brcomkassin.database.SQLiteManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockLimiter {

    public static final List<String> CONFIGURATOR_BLOCKS = new ArrayList<>();

    public static void addLimitedBlock(Player player, ItemStack itemStack, int limit) throws SQLException {
        String itemId = itemStack.getType().name();

        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            Message.Chat.send(player,
                    "&4Você precisa está segurando um item na mão para usar o comando!");
            return;
        }
        if (isLimitedBlock(itemId)) {
            Message.Chat.send(player, "&4O item já está limitado!");
            return;
        }

        saveBlockLimit(itemId, limit);
        int playerCount = getBlockCount(player, itemId);
        LimiterInventory.addItemInInventory(itemId, playerCount, limit);
        Message.Chat.send(player, "&aO item foi limitado em &6" + limit + " &ausos!");
    }

    public static boolean isLimitedBlock(String itemId) throws SQLException {
        String query = "SELECT 1 FROM block_limit WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido na hora de limitar a quantidade de uso do bloco: " + itemId);
        }
    }

    public static boolean reachedTheLimit(Player player, String itemId) throws SQLException {
        UUID playerId = player.getUniqueId();
        int limit = getBlockLimit(itemId);
        int currentCount = getBlockCount(player, itemId);
        return currentCount == limit;
    }

    public static void punished(Player player, String itemId) throws SQLException {
        UUID playerId = player.getUniqueId();
        int limit = getBlockLimit(itemId);
        int currentCount = getBlockCount(player, itemId);

        if (currentCount > limit) {
            executeCommand("ban " + player.getName() + " §4Você foi banido por burlar o limite de blocos.");
        }
    }

    public static void executeCommand(String command) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(console, command);
    }

    public static void incrementBlockCount(Player player, String itemID) throws SQLException {
        if (!isLimitedBlock(itemID)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int limit = getBlockLimit(itemID);
        int currentCount = getBlockCount(player, itemID);

        saveBlockCount(playerId, itemID, currentCount + 1);
        int updatedCount = getBlockCount(player, itemID);
    }

    public static void decrementBlockCount(Player player, String itemId) throws SQLException {
        UUID playerId = player.getUniqueId();

        int limit = getBlockLimit(itemId);
        int currentCount = getBlockCount(player, itemId);

        if (!isLimitedBlock(itemId)) {
            return;
        }
        if (currentCount == 0) return;

        saveBlockCount(playerId, itemId, currentCount - 1);
        int updatedCount = getBlockCount(player, itemId);
    }

    private static void saveBlockLimit(String itemId, int limit) throws SQLException {
        String query = "INSERT OR REPLACE INTO block_limit (item_id, block_limit_value) VALUES (?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            ps.setInt(2, limit);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido na hora de salvar os limites de blocos.");
        }
    }

    public static void removeBlockLimit(Player player) throws SQLException {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cVocê não está segurando nenhum item válido.");
            return;
        }
        String itemId = itemInHand.getType().name();

        if (!isLimitedBlock(itemId)) {
            player.sendMessage("§cEste item não possui um limite definido.");
            return;
        }
        String query = "DELETE FROM block_limit WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            ps.executeUpdate();
            player.sendMessage("§aO limite do item " + itemId + " foi removido com sucesso.");
            LimiterInventory.removeItemInInventory(itemInHand);
        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido ao remover o limite do bloco.");
        }
    }

    public static int getBlockLimit(String itemId) throws SQLException {
        String query = "SELECT block_limit_value FROM block_limit WHERE item_id = ?";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("block_limit_value");
                }
            }

        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido na hora de retornar o valor correspondente ao limite de blocos.");
        }
        return 0;
    }

    private static void saveBlockCount(UUID playerId, String itemId, int count) throws SQLException {
        String query = "INSERT OR REPLACE INTO block_count (player_uuid, item_id, count) VALUES (?, ?, ?)";
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setInt(3, count);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido na hora salvar o uso de blocos no banco de dados.");
        }
    }

    public static int getBlockCount(Player player, String itemId) throws SQLException {
        String query = "SELECT count FROM block_count WHERE player_uuid = ? AND item_id = ?";
        UUID playerId = player.getUniqueId();
        try (PreparedStatement ps = SQLiteManager.getConnection().prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            throw new SQLException("Houve um problema desconhecido na hora de retornar o valor correspondente ao uso de blocos do jogador: " + player.getName());
        }
        return 0;
    }

}
