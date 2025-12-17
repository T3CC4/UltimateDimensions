package de.tecca.ultimatedimensions.dimension;

import de.tecca.ultimatedimensions.UltimateDimensions;
import de.tecca.ultimatedimensions.generator.ChunkZoneCache;
import org.bukkit.Material;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseDimensionGenerator extends ChunkGenerator {

    protected final UltimateDimensions plugin;
    protected final DimensionConfig config;
    protected final long seed;
    protected final Map<Long, SimplexOctaveGenerator> noiseCache;
    protected ChunkZoneCache zoneCache;

    public BaseDimensionGenerator(UltimateDimensions plugin, DimensionConfig config, long seed) {
        this.plugin = plugin;
        this.config = config;
        this.seed = seed;
        this.noiseCache = new ConcurrentHashMap<>();
    }

    @Override
    @Nullable
    public abstract BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo);

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        if (zoneCache == null && getDefaultBiomeProvider(worldInfo) != null) {
            initializeZoneCache(worldInfo);
        }

        generateTerrainNoise(worldInfo, random, chunkX, chunkZ, chunkData);
    }

    protected abstract void generateTerrainNoise(WorldInfo worldInfo, Random random,
                                                 int chunkX, int chunkZ, ChunkData chunkData);

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        generateSurfaceFeatures(worldInfo, random, chunkX, chunkZ, chunkData);
    }

    protected abstract void generateSurfaceFeatures(WorldInfo worldInfo, Random random,
                                                    int chunkX, int chunkZ, ChunkData chunkData);

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, chunkData.getMinHeight(), z, Material.BEDROCK);

                int topBedrock = chunkData.getMaxHeight() - random.nextInt(5) - 1;
                for (int y = topBedrock; y < chunkData.getMaxHeight(); y++) {
                    if (random.nextInt(chunkData.getMaxHeight() - y) < 3) {
                        chunkData.setBlock(x, y, z, Material.BEDROCK);
                    }
                }
            }
        }
    }

    protected SimplexOctaveGenerator getNoiseGenerator(long baseSeed, double scale) {
        long finalSeed = seed + baseSeed;
        return noiseCache.computeIfAbsent(finalSeed, s -> {
            SimplexOctaveGenerator gen = new SimplexOctaveGenerator(new Random(s), 6);
            gen.setScale(scale);
            return gen;
        });
    }

    protected boolean generateCaves(ChunkData chunkData, int x, int y, int z,
                                    int absX, int absZ, SimplexOctaveGenerator caveNoise,
                                    int floorHeight, boolean isCeiling, double caveThreshold) {

        double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);

        if (isCeiling) {
            return caveValue > caveThreshold && y < chunkData.getMaxHeight() - 10;
        }

        return caveValue > caveThreshold &&
                y > chunkData.getMinHeight() + 5 &&
                y < floorHeight - 3;
    }

    protected abstract void initializeZoneCache(WorldInfo worldInfo);

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return true; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateStructures() { return config.hasFeature("structures"); }
    @Override public boolean shouldGenerateMobs() { return config.hasFeature("mobs"); }

    public DimensionConfig getConfig() { return config; }
    public ChunkZoneCache getZoneCache() { return zoneCache; }
}