package brcomkassin.blockLimiter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum ConstructionWandItems {

    DIAMOND_WAND("constructionwand_diamond_wand"),
    IRON_WAND("constructionwand_iron_wand"),
    STONE_WAND("constructionwand_stone_wand"),
    INFINITY_WAND("constructionwand_infinity_wand");

    private final String name;

    private static final Map<String, ConstructionWandItems> ITEM_MAP = new HashMap<>();

    static {
        for (ConstructionWandItems item : values()) {
            ITEM_MAP.put(item.name.toLowerCase(), item);
        }
    }

    public static boolean isWandItem(String name) {
        return ITEM_MAP.containsKey(name.toLowerCase());
    }
}
