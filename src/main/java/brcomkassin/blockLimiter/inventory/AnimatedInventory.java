package brcomkassin.blockLimiter.inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockGroup;

public class AnimatedInventory {
    private static final Map<Integer, BlockGroup> SLOT_GROUP_MAP = new HashMap<>();
    private static final Map<Integer, AtomicInteger> ANIMATION_INDEXES = new HashMap<>();
    private static BukkitRunnable animationTask;

    public static void registerSlot(int slot, BlockGroup group) {
        SLOT_GROUP_MAP.put(slot, group);
        ANIMATION_INDEXES.put(slot, new AtomicInteger(0));
    }

    public static void clearSlots() {
        SLOT_GROUP_MAP.clear();
        ANIMATION_INDEXES.clear();
    }

    public static Material getCurrentMaterial(int slot) {
        BlockGroup group = SLOT_GROUP_MAP.get(slot);
        if (group == null) return null;

        List<Material> materials = group.getMaterials().stream().toList();
        if (materials.isEmpty()) return null;

        int currentIndex = ANIMATION_INDEXES.get(slot).get() % materials.size();
        return materials.get(currentIndex);
    }

    public static void startAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
        }

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                ANIMATION_INDEXES.forEach((slot, index) -> {
                    index.incrementAndGet();
                    LimiterInventory.updateSlot(slot);
                });
            }
        };

        animationTask.runTaskTimer(BlockLimiterPlugin.getInstance(), 20L, 20L);
    }

    public static void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }
} 