package brcomkassin.blockLimiter.limiter;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlacedBlock {
    private final UUID playerUuid;
    private final String groupId;
    private final Material material;
    private final Location location;
    private final long placedAt;
} 