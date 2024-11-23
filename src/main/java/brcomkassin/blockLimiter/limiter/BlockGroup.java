package brcomkassin.blockLimiter.limiter;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BlockGroup {
    private final String groupId;
    private final String groupName;
    private final int limit;
    private final Set<Material> materials = new HashSet<>();

    public void addMaterial(Material material) {
        materials.add(material);
    }

    public boolean containsMaterial(Material material) {
        return materials.contains(material);
    }
} 