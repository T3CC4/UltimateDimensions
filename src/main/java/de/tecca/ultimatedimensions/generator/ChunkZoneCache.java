package de.tecca.ultimatedimensions.generator;

import de.tecca.ultimatedimensions.dimension.BaseBiomeProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkZoneCache {

    public record ZoneData(int zoneType, double densityMultiplier, double zoneBlend) {
        public String getZoneName() {
            return switch (zoneType) {
                case 1 -> "Geode-Zone";
                case 2 -> "Kristall-Feld";
                case 3 -> "Tiefe Zone";
                default -> "Normal-Zone";
            };
        }

        public boolean isGeodeZone() { return zoneType == 1; }
        public boolean isCrystalField() { return zoneType == 2; }
        public boolean isDeepZone() { return zoneType == 3; }
        public boolean isNormalZone() { return zoneType == 0; }
    }

    private final Map<Long, ZoneData> cache = new ConcurrentHashMap<>();
    private final BaseBiomeProvider biomeProvider;

    public ChunkZoneCache(BaseBiomeProvider biomeProvider) {
        this.biomeProvider = biomeProvider;
    }

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public ZoneData getZoneData(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long key = getChunkKey(chunkX, chunkZ);

        return cache.computeIfAbsent(key, k -> {
            int centerX = (chunkX << 4) + 8;
            int centerZ = (chunkZ << 4) + 8;

            int zoneType = biomeProvider.getZoneType(centerX, centerZ);
            double zoneBlend = biomeProvider.getZoneBlend(centerX, centerZ);
            double densityMultiplier = getDensityMultiplier(zoneType);

            return new ZoneData(zoneType, densityMultiplier, zoneBlend);
        });
    }

    public ZoneData getZoneDataPrecise(int x, int z) {
        int zoneType = biomeProvider.getZoneType(x, z);
        double zoneBlend = biomeProvider.getZoneBlend(x, z);
        double densityMultiplier = getDensityMultiplier(zoneType);
        return new ZoneData(zoneType, densityMultiplier, zoneBlend);
    }

    private double getDensityMultiplier(int zoneType) {
        return switch (zoneType) {
            case 1 -> 1.8;
            case 2 -> 1.4;
            default -> 1.0;
        };
    }

    public void clear() { cache.clear(); }
    public int size() { return cache.size(); }

    public void cleanup(int maxSize) {
        if (cache.size() > maxSize) {
            int toRemove = cache.size() / 2;
            cache.keySet().stream().limit(toRemove).forEach(cache::remove);
        }
    }
}