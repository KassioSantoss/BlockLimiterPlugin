package brcomkassin.blockLimiter.limiter;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;

public record PlacedBlock(UUID playerUuid, String groupId, Material material, Location location, long placedAt) {} 