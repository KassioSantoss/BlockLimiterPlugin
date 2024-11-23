package brcomkassin.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;

public class SQLiteManager {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    
    @Getter
    private static Connection connection;

    public static void connectAndCreateTables() {
        try {
            if (connection != null) return;

            File databaseFile = new File("plugins/BlockLimiter/data.db");

            if (!databaseFile.exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            String url = "jdbc:sqlite:" + databaseFile.getPath();
            connection = DriverManager.getConnection(url);
            createTables();
            LOGGER.info("Conectado ao SQLite com sucesso!");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao conectar ao SQLite", e);
        }
    }

    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Conexão com SQLite fechada com sucesso!");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao fechar conexão com SQLite", e);
        }
    }

    public static void createTables() {
        try (PreparedStatement ps1 = connection.prepareStatement(SQLiteQueries.BLOCK_LIMIT_TABLE.getString());
             PreparedStatement ps2 = connection.prepareStatement(SQLiteQueries.BLOCK_GROUP_ITEMS_TABLE.getString());
             PreparedStatement ps3 = connection.prepareStatement(SQLiteQueries.PLACED_BLOCKS_TABLE.getString());
             PreparedStatement ps4 = connection.prepareStatement(SQLiteQueries.BLOCK_HISTORY_TABLE.getString())) {

            ps1.executeUpdate();
            ps2.executeUpdate();
            ps3.executeUpdate();
            ps4.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao criar tabelas no SQLite", e);
        }
    }
}

