package brcomkassin.blockLimiter;

import java.util.HashMap;
import java.util.Map;

public class Limiter {

    private final static Map<String, Integer> BLOCK_COUNTER = new HashMap<>();

    public int getBlockCount(String itemId) {
        return BLOCK_COUNTER.getOrDefault(itemId, 0);
    }

    public void incrementBlockCount(String itemId) {
        BLOCK_COUNTER.put(itemId, getBlockCount(itemId) + 1);
    }

}
