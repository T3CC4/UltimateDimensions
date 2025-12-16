package de.tecca.ultimatedimensions.generator;

import de.tecca.ultimatedimensions.UltimateDimensions;
import de.tecca.ultimatedimensions.util.OraxenIntegration;
import org.bukkit.Material;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AmethystDimensionGenerator extends ChunkGenerator {

    private final UltimateDimensions plugin;
    private final Map<Long, SimplexOctaveGenerator> noiseCache = new ConcurrentHashMap<>();
    private OraxenIntegration oraxenIntegration;
    private AmethystBiomeProvider biomeProvider;
    private boolean debugMode = false;

    public AmethystDimensionGenerator(UltimateDimensions plugin) {
        this.plugin = plugin;
        this.debugMode = plugin.getConfig().getBoolean("debug", false);

        if (plugin.isOraxenAvailable()) {
            try {
                this.oraxenIntegration = new OraxenIntegration();
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden der Oraxen-Integration: " + e.getMessage());
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
            if (debugMode) {
                plugin.getLogger().info("BiomeProvider initialisiert mit Seed: " + worldInfo.getSeed());
            }
        }
        return biomeProvider;
    }

    public AmethystBiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    private SimplexOctaveGenerator getNoiseGenerator(long seed, double scale) {
        return noiseCache.computeIfAbsent(seed, s -> {
            SimplexOctaveGenerator gen = new SimplexOctaveGenerator(new Random(s), 6);
            gen.setScale(scale);
            return gen;
        });
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        // BiomeProvider sicherstellen
        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
        }

        SimplexOctaveGenerator floorNoise = getNoiseGenerator(worldInfo.getSeed(), 0.015);
        SimplexOctaveGenerator ceilingNoise = getNoiseGenerator(worldInfo.getSeed() + 1000, 0.012);
        SimplexOctaveGenerator pillarNoise = getNoiseGenerator(worldInfo.getSeed() + 2000, 0.05);
        SimplexOctaveGenerator oreNoise = getNoiseGenerator(worldInfo.getSeed() + 3000, 0.08);
        SimplexOctaveGenerator crystalNoise = getNoiseGenerator(worldInfo.getSeed() + 4000, 0.03);
        SimplexOctaveGenerator geodeNoise = getNoiseGenerator(worldInfo.getSeed() + 5000, 0.02);

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        // Debug: Ersten Chunk loggen
        if (debugMode && chunkX == 0 && chunkZ == 0) {
            int centerX = worldX + 8;
            int centerZ = worldZ + 8;
            int zoneType = biomeProvider.getZoneType(centerX, centerZ);
            plugin.getLogger().info("Chunk 0,0 - Zone Type: " + zoneType + " (" + biomeProvider.getZoneName(centerX, centerZ) + ")");
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                // Biom-basierte Modifikatoren
                double densityMultiplier = biomeProvider.getAmethystDensityMultiplier(absX, absZ);
                boolean isGeodeZone = biomeProvider.isGeodeZone(absX, absZ);
                boolean isCrystalField = biomeProvider.isCrystalField(absX, absZ);

                // Nether-ähnlicher Boden mit Biom-Variation
                double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
                int baseFloorHeight = (int) (floorValue * 30) + 32;

                // Geode-Zonen haben niedrigeren Boden für mehr Raum
                int floorHeight = isGeodeZone ? baseFloorHeight - 5 : baseFloorHeight;

                // Kristall-Felder haben höhere Decken
                double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
                int baseCeilingStart = (int) (ceilingValue * 25) + 95;
                int ceilingStart = isCrystalField ? baseCeilingStart + 8 : baseCeilingStart;

                // Geode-Zonen verwenden geodeNoise
                double geodeValue = isGeodeZone ? geodeNoise.noise(absX, absZ, 0.5, 0.5, true) : 0.0;
                boolean isGeodeCore = geodeValue > 0.6;

                // Boden generieren
                for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
                    Material block = selectAmethystBlock(random, y, floorHeight, absX, y, absZ,
                            oreNoise, crystalNoise,
                            isGeodeCore, true,
                            densityMultiplier, isCrystalField);
                    chunkData.setBlock(x, y, z, block);
                }

                // Decke generieren
                for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
                    Material block = selectAmethystBlock(random, y, ceilingStart, absX, y, absZ,
                            oreNoise, crystalNoise,
                            isGeodeCore, false,
                            densityMultiplier, isCrystalField);
                    chunkData.setBlock(x, y, z, block);
                }

                // Amethyst-Säulen - mehr in Geode-Zonen
                double pillarValue = pillarNoise.noise(absX, absZ, 1, 1, true);
                double pillarThreshold = isGeodeZone ? 0.55 : (isCrystalField ? 0.70 : 0.65);

                if (pillarValue > pillarThreshold) {
                    Material pillarMaterial = pillarValue > 0.85 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;

                    for (int y = floorHeight; y < ceilingStart; y++) {
                        // Mehr Kristall-Verzierungen in Kristall-Feldern
                        int crystalChance = isCrystalField ? 25 : 15;
                        if (random.nextInt(100) < crystalChance) {
                            chunkData.setBlock(x, y, z, Material.AMETHYST_CLUSTER);
                        } else {
                            chunkData.setBlock(x, y, z, pillarMaterial);
                        }
                    }
                }

                // Schwebende Amethyst-Inseln - nur in Geode-Zonen
                if (isGeodeZone) {
                    double islandValue = crystalNoise.noise(absX, 50, absZ, 0.5, 0.5, true);
                    if (islandValue > 0.72) {
                        int islandY = 60 + (int)(islandValue * 20);
                        int islandSize = random.nextInt(2) + 2;

                        for (int dy = 0; dy < islandSize; dy++) {
                            if (islandY + dy < ceilingStart && islandY + dy > floorHeight) {
                                Material islandBlock = dy == islandSize - 1 && random.nextBoolean()
                                        ? Material.BUDDING_AMETHYST
                                        : Material.AMETHYST_BLOCK;
                                chunkData.setBlock(x, islandY + dy, z, islandBlock);
                            }
                        }
                    }
                }

                // AMETHYST-SPIKES: Große Stalaktiten/Stalagmiten
                double spikeValue = pillarNoise.noise(absX * 2, absZ * 2, 0.5, 0.5, true);

                // Stalaktiten (von Decke hängend)
                if (spikeValue > 0.82) {
                    int spikeHeight = random.nextInt(8) + 5;
                    int spikeStartY = ceilingStart - 1;

                    for (int dy = 0; dy < spikeHeight; dy++) {
                        int currentY = spikeStartY - dy;
                        if (currentY > floorHeight + 10) {
                            // Spike wird schmaler nach unten
                            if (dy < spikeHeight - 2 || random.nextBoolean()) {
                                Material spikeMaterial = dy < 2 && random.nextInt(100) < 30
                                        ? Material.BUDDING_AMETHYST
                                        : Material.AMETHYST_BLOCK;
                                chunkData.setBlock(x, currentY, z, spikeMaterial);

                                // Cluster an Spike-Spitze
                                if (dy == spikeHeight - 1 && random.nextInt(100) < 40) {
                                    chunkData.setBlock(x, currentY, z, Material.AMETHYST_CLUSTER);
                                }
                            }
                        }
                    }
                }

                // Stalagmiten (vom Boden aufragend)
                if (spikeValue < -0.82) {
                    int spikeHeight = random.nextInt(10) + 6;
                    int spikeStartY = floorHeight;

                    for (int dy = 0; dy < spikeHeight; dy++) {
                        int currentY = spikeStartY + dy;
                        if (currentY < ceilingStart - 10) {
                            // Spike wird schmaler nach oben
                            if (dy < spikeHeight - 2 || random.nextBoolean()) {
                                Material spikeMaterial = dy < 3 && random.nextInt(100) < 25
                                        ? Material.BUDDING_AMETHYST
                                        : Material.AMETHYST_BLOCK;
                                chunkData.setBlock(x, currentY, z, spikeMaterial);

                                // Cluster an Spike-Spitze
                                if (dy == spikeHeight - 1 && random.nextInt(100) < 50) {
                                    chunkData.setBlock(x, currentY, z, Material.AMETHYST_CLUSTER);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Material selectAmethystBlock(Random random, int y, int surfaceLevel,
                                         int worldX, int worldY, int worldZ,
                                         SimplexOctaveGenerator oreNoise,
                                         SimplexOctaveGenerator crystalNoise,
                                         boolean isGeodeZone,
                                         boolean isFloor,
                                         double densityMultiplier,
                                         boolean isCrystalField) {

        int depth = Math.abs(y - surfaceLevel);
        double crystalValue = crystalNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);

        // Oraxen Custom Ore-Generierung (reduziert durch densityMultiplier)
        if (oraxenIntegration != null && depth > 3 && depth < 25) {
            double oreValue = oreNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);

            // Weniger Ores in Geode-Zonen/Kristall-Feldern
            double oreThreshold = isGeodeZone || isCrystalField ? 0.95 : 0.92;

            if (depth <= 8) {
                if (oreValue > oreThreshold) {
                    Material oraxenOre = oraxenIntegration.getRandomOre(random, "common");
                    if (oraxenOre != null) return oraxenOre;
                }
            } else if (depth <= 16) {
                if (oreValue > oreThreshold + 0.02) {
                    Material oraxenOre = oraxenIntegration.getRandomOre(random, "rare");
                    if (oraxenOre != null) return oraxenOre;
                }
            } else {
                if (oreValue > oreThreshold + 0.03) {
                    Material oraxenOre = oraxenIntegration.getRandomOre(random, "epic");
                    if (oraxenOre != null) return oraxenOre;
                }
            }
        }

        // GEODE-ZONEN: Fast nur Amethyst (mit densityMultiplier)
        if (isGeodeZone && depth > 2 && depth < 20) {
            int roll = random.nextInt(100);
            int amethystChance = (int)(70 * densityMultiplier);

            if (roll < amethystChance) return Material.AMETHYST_BLOCK;
            if (roll < amethystChance + 15) return Material.BUDDING_AMETHYST;
            if (roll < amethystChance + 20) return Material.SMOOTH_BASALT;
            return Material.TUFF;
        }

        // KRISTALL-FELDER: Mehr Budding Amethyst
        if (isCrystalField && depth > 1 && depth < 15) {
            int roll = random.nextInt(100);
            if (roll < 40) return Material.BUDDING_AMETHYST;
            if (roll < 75) return Material.AMETHYST_BLOCK;
            if (roll < 90) return Material.TUFF;
            return Material.SMOOTH_BASALT;
        }

        // OBERFLÄCHE: Viel Budding Amethyst
        if (depth <= 1) {
            int buddingChance = (int)(40 * densityMultiplier);
            return random.nextInt(100) < buddingChance
                    ? Material.BUDDING_AMETHYST
                    : Material.AMETHYST_BLOCK;
        }

        // DIREKT UNTER OBERFLÄCHE
        if (depth <= 3) {
            int roll = random.nextInt(100);
            int amethystChance = (int)(65 * densityMultiplier);

            if (roll < amethystChance) return Material.AMETHYST_BLOCK;
            if (roll < amethystChance + 20) return Material.BUDDING_AMETHYST;
            if (roll < amethystChance + 25) return Material.TUFF;
            return Material.SMOOTH_BASALT;
        }

        // MITTLERE TIEFE
        if (depth <= 10) {
            if (crystalValue > 0.7) {
                return Material.BUDDING_AMETHYST;
            }

            int roll = random.nextInt(100);
            int amethystChance = (int)(50 * densityMultiplier);

            if (roll < amethystChance) return Material.AMETHYST_BLOCK;
            if (roll < 70) return Material.TUFF;
            if (roll < 85) return Material.SMOOTH_BASALT;
            if (roll < 95) return Material.DEEPSLATE;
            return Material.BLACKSTONE;
        }

        // TIEFE SCHICHTEN
        if (depth <= 20) {
            if (crystalValue > 0.75) {
                return Material.AMETHYST_BLOCK;
            }

            int roll = random.nextInt(100);
            if (roll < 35) return Material.AMETHYST_BLOCK;
            if (roll < 55) return Material.TUFF;
            if (roll < 70) return Material.SMOOTH_BASALT;
            if (roll < 85) return Material.DEEPSLATE;
            return Material.BLACKSTONE;
        }

        // KERN
        if (crystalValue > 0.8) {
            return Material.AMETHYST_BLOCK;
        }

        int roll = random.nextInt(100);
        if (roll < 25) return Material.AMETHYST_BLOCK;
        if (roll < 45) return Material.TUFF;
        if (roll < 65) return Material.DEEPSLATE;
        if (roll < 85) return Material.BLACKSTONE;
        return Material.SMOOTH_BASALT;
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        // BiomeProvider sicherstellen
        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
        }

        SimplexOctaveGenerator clusterNoise = getNoiseGenerator(worldInfo.getSeed() + 6000, 0.04);
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                // Biom-basierte Cluster-Dichte
                boolean isGeodeZone = biomeProvider.isGeodeZone(absX, absZ);
                boolean isCrystalField = biomeProvider.isCrystalField(absX, absZ);

                // Cluster-Dichte basierend auf Noise und Biom
                double clusterDensity = clusterNoise.noise(absX, absZ, 0.5, 0.5, true);
                boolean highDensity = clusterDensity > 0.6 || isCrystalField;

                // Boden-Oberfläche
                for (int y = 80; y > chunkData.getMinHeight(); y--) {
                    if (chunkData.getType(x, y, z) != Material.AIR
                            && chunkData.getType(x, y + 1, z) == Material.AIR) {

                        // Mehr Cluster in speziellen Biomen
                        int clusterChance = isCrystalField ? 60 : (isGeodeZone ? 45 : (highDensity ? 40 : 25));

                        if (random.nextInt(100) < clusterChance) {
                            placeAmethystCluster(chunkData, x, y + 1, z, random, highDensity || isCrystalField);
                        }

                        // Calcite-Kristalle als Variation (REDUZIERT)
                        if (random.nextInt(100) < 2 && !isCrystalField) {
                            chunkData.setBlock(x, y + 1, z, Material.CALCITE);
                        }
                        break;
                    }
                }

                // Decken-Oberfläche (von unten) - mehr in Geode-Zonen
                for (int y = 90; y < chunkData.getMaxHeight(); y++) {
                    if (chunkData.getType(x, y, z) != Material.AIR
                            && chunkData.getType(x, y - 1, z) == Material.AIR) {

                        // Hängende Cluster - besonders viele in Geode-Zonen
                        int hangingChance = isGeodeZone ? 50 : 30;
                        if (random.nextInt(100) < hangingChance) {
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
                Material.AMETHYST_CLUSTER // Mehr volle Cluster
        };

        if (y >= data.getMinHeight() && y < data.getMaxHeight()) {
            Material[] clusters = large ? largeClusters : smallClusters;
            data.setBlock(x, y, z, clusters[random.nextInt(clusters.length)]);
        }
    }

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

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return true; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateStructures() { return true; }  // FIX: Verhindert Freeze bei locateNearestStructure()
    @Override public boolean shouldGenerateMobs() { return true; }
}