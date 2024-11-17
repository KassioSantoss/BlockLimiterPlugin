package brcomkassin.database;

import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLiteManager {

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
            System.out.println("Conectado ao SQLite!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexão com SQLite fechada!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTables() {
        String createBlockLimitTable = SQLiteType.BLOCK_LIMIT_TABLE.getString();

        String createBlockCountTable = SQLiteType.BLOCK_COUNT_TABLE.getString();

        try (PreparedStatement ps1 = connection.prepareStatement(createBlockLimitTable);
             PreparedStatement ps2 = connection.prepareStatement(createBlockCountTable)) {

            ps1.executeUpdate();
            ps2.executeUpdate();
            System.out.println("Tabelas criadas com sucesso!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

