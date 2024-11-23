package brcomkassin.blockLimiter.inventory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import brcomkassin.BlockLimiterPlugin;
import brcomkassin.blockLimiter.limiter.BlockGroup;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.utils.ItemBuilder;

public class AnimatedInventory {
    private static final Logger LOGGER = Logger.getLogger("BlockLimiter");
    public static final Map<Integer, BlockGroup> SLOT_GROUP_MAP = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> PLAYER_ANIMATIONS = new HashMap<>();
    private static final Map<UUID, Map<Integer, AtomicInteger>> PLAYER_INDEXES = new HashMap<>();

    public static void registerSlot(int slot, BlockGroup group, Player player) {
        SLOT_GROUP_MAP.put(slot, group);
        
        Map<Integer, AtomicInteger> playerIndexes = PLAYER_INDEXES.computeIfAbsent(
            player.getUniqueId(), 
            k -> new HashMap<>()
        );
        playerIndexes.put(slot, new AtomicInteger(0));
    }

    public static void clearSlots() {
        SLOT_GROUP_MAP.clear();
    }

    public static Material getCurrentMaterial(int slot, UUID playerUuid) {
        BlockGroup group = SLOT_GROUP_MAP.get(slot);
        if (group == null) {
            return null;
        }

        List<Material> materials = new ArrayList<>(group.getMaterials());
        if (materials.isEmpty()) {
            return null;
        }

        Map<Integer, AtomicInteger> indexes = PLAYER_INDEXES.computeIfAbsent(playerUuid, k -> new HashMap<>());
        AtomicInteger index = indexes.computeIfAbsent(slot, k -> new AtomicInteger(0));
        
        int currentIndex = index.get() % materials.size();
        Material material = materials.get(currentIndex);
        return material;
    }

    public static void startAnimation(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        BukkitRunnable existingTask = PLAYER_ANIMATIONS.remove(playerUuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopAnimation(player);
                    return;
                }

                Map<Integer, AtomicInteger> currentIndexes = PLAYER_INDEXES.get(playerUuid);
                if (currentIndexes != null && !currentIndexes.isEmpty()) {
                    currentIndexes.forEach((slot, index) -> {
                        index.incrementAndGet();
                        try {
                            updateSlot(player, slot);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Erro ao atualizar slot durante animação", e);
                            stopAnimation(player);
                        }
                    });
                }
            }
        };

        task.runTaskTimer(BlockLimiterPlugin.getInstance(), 30L, 30L);
        PLAYER_ANIMATIONS.put(playerUuid, task);
    }

    public static void stopAnimation(Player player) {
        UUID playerUuid = player.getUniqueId();
        BukkitRunnable task = PLAYER_ANIMATIONS.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
        PLAYER_INDEXES.remove(playerUuid);
    }

    private static void updateSlot(Player player, int slot) {
        Material currentMaterial = getCurrentMaterial(slot, player.getUniqueId());
        if (currentMaterial == null) return;

        BlockGroup group = SLOT_GROUP_MAP.get(slot);
        if (group == null) return;

        try {
            List<String> lore = new ArrayList<>();
            lore.add("&7Seus blocos: &a" + BlockLimiter.getPlacedBlockCount(player.getUniqueId(), group.getGroupId()) + " &7/ &c" + group.getLimit());
            lore.add("");
            lore.add("&7Blocos neste grupo:");

            group.getMaterials().forEach(material -> {
                String prefix = material.equals(currentMaterial) ? "&a" : "&7";
                lore.add("&8- " + prefix + formatMaterialName(material.name()));
            });

            ItemStack item = new ItemBuilder(currentMaterial)
                    .setName("&6" + formatGroupName(group.getGroupName()))
                    .setLore(lore)
                    .build();

            player.getOpenInventory().getTopInventory().setItem(slot, item);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar slot do inventário", e);
        }
    }

    private static String formatMaterialName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String formatGroupName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static void debugState(Player player) {
        UUID playerUuid = player.getUniqueId();
        LOGGER.log(Level.INFO, "=== Debug do estado da animação ===");
        LOGGER.log(Level.INFO, "Jogador: {0}", player.getName());
        LOGGER.log(Level.INFO, "Tem animação ativa: {0}", PLAYER_ANIMATIONS.containsKey(playerUuid));
        
        Map<Integer, AtomicInteger> indexes = PLAYER_INDEXES.get(playerUuid);
        if (indexes != null) {
            LOGGER.log(Level.INFO, "Slots registrados: {0}", indexes.keySet());
            indexes.forEach((slot, index) -> {
                BlockGroup group = SLOT_GROUP_MAP.get(slot);
                LOGGER.log(Level.INFO, "Slot {0}: Grupo={1}, Índice={2}", 
                    new Object[]{slot, group != null ? group.getGroupName() : "null", index.get()});
            });
        } else {
            LOGGER.log(Level.INFO, "Nenhum slot registrado");
        }
        LOGGER.log(Level.INFO, "===============================");
    }
} 