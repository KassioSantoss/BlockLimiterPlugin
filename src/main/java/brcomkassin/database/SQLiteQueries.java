package brcomkassin.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SQLiteQueries {

    BLOCK_LIMIT_TABLE("CREATE TABLE IF NOT EXISTS block_limit (" +
            "item_id TEXT PRIMARY KEY, " +
            "block_limit_value INTEGER NOT NULL"),

    BLOCK_COUNT_TABLE("CREATE TABLE IF NOT EXISTS block_count (" +
            "player_uuid TEXT, " +
            "item_id TEXT, " +
            "count INTEGER NOT NULL, " +
            "PRIMARY KEY (player_uuid, item_id))");

    private final String string;

}
