package de.tecca.ultimatedimensions.dimensions.amethyst;

import de.tecca.ultimatedimensions.dimension.BaseBiomeProvider;
import org.bukkit.block.Biome;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class AmethystBiomeProvider extends BaseBiomeProvider {

    private static final Biome BIOME_NORMAL = Biome.WARPED_FOREST;
    private static final Biome BIOME_GEODE = Biome.CRIMSON_FOREST;
    private static final Biome BIOME_CRYSTAL = Biome.SOUL_SAND_VALLEY;
    private static final Biome BIOME_DEEP = Biome.BASALT_DELTAS;

    public AmethystBiomeProvider(long seed) {
        super(seed);
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        int zoneType = getZoneType(x, z);
        return switch (zoneType) {
            case 1 -> BIOME_GEODE;
            case 2 -> BIOME_CRYSTAL;
            case 3 -> BIOME_DEEP;
            default -> BIOME_NORMAL;
        };
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return Arrays.asList(BIOME_NORMAL, BIOME_GEODE, BIOME_CRYSTAL, BIOME_DEEP);
    }

    @Override
    public int getZoneType(int x, int z) {
        double combined = getCombinedNoise(x, z);

        if (combined < -0.5) {
            return 2; // Crystal
        } else if (combined >= -0.1 && combined < 0.35) {
            return 1; // Geode
        } else if (combined >= 0.55) {
            return 3; // Deep
        } else {
            return 0; // Normal
        }
    }

    @Override
    public double getZoneBlend(int x, int z) {
        double combined = getCombinedNoise(x, z);
        int zoneType = getZoneType(x, z);

        double distance = switch (zoneType) {
            case 0 -> Math.abs(combined - 0.2);
            case 1 -> Math.abs(combined - 0.12);
            case 2 -> Math.abs(combined + 0.7);
            case 3 -> Math.abs(combined - 0.7);
            default -> 0;
        };

        return Math.max(0.0, 1.0 - (distance / 0.3));
    }

    @Override
    public String getZoneName(int x, int z) {
        int zone = getZoneType(x, z);
        return switch (zone) {
            case 1 -> "Geode Zone";
            case 2 -> "Crystal Field";
            case 3 -> "Deep Zone";
            default -> "Normal Zone";
        };
    }

    public double getAmethystDensityMultiplier(int x, int z) {
        int zone = getZoneType(x, z);
        return switch (zone) {
            case 1 -> 1.8;
            case 2 -> 1.4;
            default -> 1.0;
        };
    }
}