package brcomkassin.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SQLiteQueries {

    BLOCK_LIMIT_TABLE(
        "CREATE TABLE IF NOT EXISTS block_limit (" +
        "group_id TEXT PRIMARY KEY, " +
        "group_name TEXT NOT NULL, " +
        "block_limit_value INT NOT NULL)"
    ),

    BLOCK_GROUP_ITEMS_TABLE(
        "CREATE TABLE IF NOT EXISTS block_group_items (" +
        "group_id TEXT, " +
        "item_id TEXT, " +
        "PRIMARY KEY (group_id, item_id), " +
        "FOREIGN KEY (group_id) REFERENCES block_limit(group_id))"
    ),

    PLACED_BLOCKS_TABLE(
        "CREATE TABLE IF NOT EXISTS placed_blocks (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "player_uuid TEXT NOT NULL, " +
        "group_id TEXT NOT NULL, " +
        "item_id TEXT NOT NULL, " +
        "world TEXT NOT NULL, " +
        "x INT NOT NULL, " +
        "y INT NOT NULL, " +
        "z INT NOT NULL, " +
        "placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (group_id) REFERENCES block_limit(group_id))"
    ),

    BLOCK_HISTORY_TABLE(
        "CREATE TABLE IF NOT EXISTS block_history (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "player_uuid TEXT NOT NULL, " +
        "group_id TEXT NOT NULL, " +
        "item_id TEXT NOT NULL, " +
        "world TEXT NOT NULL, " +
        "x INT NOT NULL, " +
        "y INT NOT NULL, " +
        "z INT NOT NULL, " +
        "action TEXT NOT NULL, " + 
        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (group_id) REFERENCES block_limit(group_id))"
    );

    private final String string;
}
