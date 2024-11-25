package brcomkassin.blockLimiter.utils;

import brcomkassin.blockLimiter.limiter.BlockGroup;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryItemBuilder {
    
    public static ItemStack buildGroupItem(BlockGroup group, Material currentMaterial, int blockCount) {
        String groupName = formatGroupName(group.getGroupName());
        List<String> lore = buildGroupLore(group, currentMaterial, blockCount);
        
        return new ItemBuilder(currentMaterial)
                .setName("&6" + groupName)
                .setLore(lore)
                .build();
    }
    
    private static List<String> buildGroupLore(BlockGroup group, Material currentMaterial, int blockCount) {
        List<String> lore = new ArrayList<>();
        
        lore.add("&7Blocos no chão: &a" + blockCount + " &7/ &c" + group.getLimit());
        lore.add("");
        lore.add("&7Blocos neste grupo:");
        
        group.getMaterials().forEach(material -> {
            String prefix = material.equals(currentMaterial) ? "&a" : "&7";
            lore.add("&8- " + prefix + formatMaterialName(material.name()));
        });
        
        return lore;
    }
    
    public static String formatMaterialName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
    
    public static String formatGroupName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
} 