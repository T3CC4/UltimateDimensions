package de.tecca.ultimatedimensions.dimensions.amethyst;

import de.tecca.ultimatedimensions.UltimateDimensions;
import de.tecca.ultimatedimensions.dimension.BaseDimensionGenerator;
import de.tecca.ultimatedimensions.dimension.DimensionConfig;
import de.tecca.ultimatedimensions.generator.ChunkZoneCache;
import de.tecca.ultimatedimensions.generator.TerrainConstants;
import de.tecca.ultimatedimensions.generator.BlockSelector;
import de.tecca.ultimatedimensions.util.OraxenIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class AmethystDimensionGenerator extends BaseDimensionGenerator {

    private final TerrainConstants constants;
    private AmethystBiomeProvider biomeProvider;
    private BlockSelector blockSelector;
    private OraxenIntegration oraxenIntegration;

    public AmethystDimensionGenerator(UltimateDimensions plugin, DimensionConfig config, long seed) {
        super(plugin, config, seed);
        this.constants = TerrainConstants.createDefault();
        this.constants.loadFromConfig(plugin.getConfig());

        if (plugin.isOraxenAvailable()) {
            initializeOraxenDelayed();
        }
    }

    @Override
    @Nullable
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
        }
        return biomeProvider;
    }

    @Override
    protected void initializeZoneCache(WorldInfo worldInfo) {
        if (biomeProvider != null) {
            zoneCache = new ChunkZoneCache(biomeProvider);
        }
    }

    @Override
    protected void generateTerrainNoise(WorldInfo worldInfo, Random random,
                                        int chunkX, int chunkZ, ChunkData chunkData) {

        if (blockSelector == null) {
            blockSelector = new BlockSelector(constants, oraxenIntegration);
        }

        SimplexOctaveGenerator floorNoise = getNoiseGenerator(0, constants.noise.floor);
        SimplexOctaveGenerator ceilingNoise = getNoiseGenerator(1000, constants.noise.ceiling);
        SimplexOctaveGenerator pillarNoise = getNoiseGenerator(2000, constants.noise.pillar);
        SimplexOctaveGenerator oreNoise = getNoiseGenerator(3000, constants.noise.ore);
        SimplexOctaveGenerator crystalNoise = getNoiseGenerator(4000, constants.noise.crystal);
        SimplexOctaveGenerator caveNoise = getNoiseGenerator(9000, constants.noise.cave);

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        ChunkZoneCache.ZoneData chunkZoneData = zoneCache.getZoneData(worldX + 8, worldZ + 8);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
                int floorHeight = (int) (floorValue * constants.normal.floorVariation) + constants.normal.floorBaseHeight;

                double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
                int ceilingStart = (int) (ceilingValue * constants.normal.ceilingVariation) + constants.normal.ceilingBase;

                for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
                    if (generateCaves(chunkData, x, y, z, absX, absZ, caveNoise, floorHeight, false, 0.6)) {
                        continue;
                    }

                    Material block = blockSelector.selectNormalBlock(random, y, floorHeight,
                            absX, y, absZ, oreNoise, crystalNoise, chunkZoneData.densityMultiplier());
                    chunkData.setBlock(x, y, z, block);
                }

                for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
                    if (generateCaves(chunkData, x, y, z, absX, absZ, caveNoise, ceilingStart, true, 0.65)) {
                        continue;
                    }

                    Material block = blockSelector.selectNormalBlock(random, y, ceilingStart,
                            absX, y, absZ, oreNoise, crystalNoise, chunkZoneData.densityMultiplier());
                    chunkData.setBlock(x, y, z, block);
                }

                double pillarValue = pillarNoise.noise(absX, absZ, 1, 1, true);
                if (pillarValue > constants.normal.pillarThreshold) {
                    Material pillarMaterial = pillarValue > constants.normal.elitePillarThreshold
                            ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;

                    int pillarTop = Math.min(ceilingStart - 5,
                            floorHeight + constants.normal.minPillarHeight +
                                    random.nextInt(constants.normal.maxPillarHeight - constants.normal.minPillarHeight));

                    for (int y = floorHeight; y < pillarTop; y++) {
                        if (random.nextInt(100) < 15) {
                            chunkData.setBlock(x, y, z, Material.AMETHYST_CLUSTER);
                        } else {
                            chunkData.setBlock(x, y, z, pillarMaterial);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void generateSurfaceFeatures(WorldInfo worldInfo, Random random,
                                           int chunkX, int chunkZ, ChunkData chunkData) {

        SimplexOctaveGenerator clusterNoise = getNoiseGenerator(8000, constants.noise.cluster);
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                double clusterDensity = clusterNoise.noise(absX, absZ, 0.5, 0.5, true);
                boolean highDensity = clusterDensity > 0.6;

                for (int y = 80; y > chunkData.getMinHeight(); y--) {
                    Material currentBlock = chunkData.getType(x, y, z);
                    Material blockAbove = chunkData.getType(x, y + 1, z);

                    if (currentBlock != Material.AIR && blockAbove == Material.AIR) {
                        if (random.nextInt(100) < (highDensity ? 40 : 25)) {
                            placeAmethystCluster(chunkData, x, y + 1, z, random, highDensity);
                        }
                        break;
                    }
                }

                for (int y = 90; y < chunkData.getMaxHeight(); y++) {
                    Material currentBlock = chunkData.getType(x, y, z);
                    Material blockBelow = chunkData.getType(x, y - 1, z);

                    if (currentBlock != Material.AIR && blockBelow == Material.AIR) {
                        if (random.nextInt(100) < 30) {
                            placeAmethystCluster(chunkData, x, y - 1, z, random, highDensity);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void placeAmethystCluster(ChunkData data, int x, int y, int z, Random random, boolean large) {
        Material[] smallClusters = {
                Material.SMALL_AMETHYST_BUD,
                Material.MEDIUM_AMETHYST_BUD,
                Material.LARGE_AMETHYST_BUD,
                Material.AMETHYST_CLUSTER
        };

        Material[] largeClusters = {
                Material.LARGE_AMETHYST_BUD,
                Material.AMETHYST_CLUSTER,
                Material.AMETHYST_CLUSTER,
                Material.AMETHYST_CLUSTER
        };

        if (y >= data.getMinHeight() && y < data.getMaxHeight()) {
            if (data.getType(x, y, z) != Material.AIR) {
                return;
            }

            Material[] clusters = large ? largeClusters : smallClusters;
            Material clusterMaterial = clusters[random.nextInt(clusters.length)];
            data.setBlock(x, y, z, clusterMaterial);
        }
    }

    private void initializeOraxenDelayed() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                this.oraxenIntegration = new OraxenIntegration();
                if (oraxenIntegration.hasOres()) {
                    plugin.getLogger().info("✓ Oraxen Custom Ores loaded for Amethyst dimension");
                } else {
                    scheduleOraxenRetry(1);
                }
            } catch (Exception e) {
                scheduleOraxenRetry(1);
            }
        }, constants.oraxen.retryDelayTicks);
    }

    private void scheduleOraxenRetry(int attempt) {
        if (attempt > 3) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                this.oraxenIntegration = new OraxenIntegration();
                if (oraxenIntegration.hasOres()) {
                    plugin.getLogger().info("✓ Oraxen loaded (attempt " + (attempt + 1) + ")");
                } else {
                    scheduleOraxenRetry(attempt + 1);
                }
            } catch (Exception e) {
                scheduleOraxenRetry(attempt + 1);
            }
        }, constants.oraxen.retryDelayTicks * attempt);
    }

    public AmethystBiomeProvider getBiomeProvider() {
        return biomeProvider;
    }
}