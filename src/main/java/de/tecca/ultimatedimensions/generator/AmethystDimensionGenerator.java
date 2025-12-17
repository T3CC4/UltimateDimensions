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

/**
 * AmethystDimensionGenerator - Custom Dimension Generator
 *
 * 4 Biome-spezifische Terrain-Typen:
 *
 * WARPED_FOREST (Normal):
 * - Ausgeglichenes Terrain mit moderaten Höhen
 * - Standard Amethyst-Dichte
 * - Kleine Amethyst-Türme und Säulen
 * - Gelegentliche Tuff-Formationen
 *
 * CRIMSON_FOREST (Geode):
 * - MASSIVE schwebende Amethyst-Inseln
 * - Sehr niedriger Boden für mehr Luftraum
 * - Riesige hohle Geoden mit Kristall-Kernen
 * - "Amethyst-Wasserfälle" (fallende Cluster)
 * - Budding Amethyst-Brücken zwischen Inseln
 *
 * SOUL_SAND_VALLEY (Kristall-Feld):
 * - Flaches Terrain übersät mit Kristall-Formationen
 * - Massive Amethyst-Cluster-"Bäume" (bis zu 20 Blöcke hoch)
 * - Kristalline Bögen und Portale
 * - "Kristall-Geysire" - vertikale Cluster-Säulen
 * - Budding Amethyst überall
 *
 * BASALT_DELTAS (Tiefe Zone):
 * - Viel Blackstone und Basalt
 * - Enge Schluchten und hohe Decken
 * - Wenig Amethyst, dafür versteckt in Adern
 * - Obsidian-Spitzen und Lava-Reste (erstarrt)
 * - Dunklere, bedrohlichere Atmosphäre
 */
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

        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
        }

        SimplexOctaveGenerator floorNoise = getNoiseGenerator(worldInfo.getSeed(), 0.015);
        SimplexOctaveGenerator ceilingNoise = getNoiseGenerator(worldInfo.getSeed() + 1000, 0.012);
        SimplexOctaveGenerator pillarNoise = getNoiseGenerator(worldInfo.getSeed() + 2000, 0.05);
        SimplexOctaveGenerator oreNoise = getNoiseGenerator(worldInfo.getSeed() + 3000, 0.08);
        SimplexOctaveGenerator crystalNoise = getNoiseGenerator(worldInfo.getSeed() + 4000, 0.03);
        SimplexOctaveGenerator geodeNoise = getNoiseGenerator(worldInfo.getSeed() + 5000, 0.02);
        SimplexOctaveGenerator islandNoise = getNoiseGenerator(worldInfo.getSeed() + 6000, 0.025);
        SimplexOctaveGenerator treeNoise = getNoiseGenerator(worldInfo.getSeed() + 7000, 0.04);
        SimplexOctaveGenerator caveNoise = getNoiseGenerator(worldInfo.getSeed() + 9000, 0.04);

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                int zoneType = biomeProvider.getZoneType(absX, absZ);
                double densityMultiplier = biomeProvider.getAmethystDensityMultiplier(absX, absZ);
                double zoneBlend = biomeProvider.getZoneBlend(absX, absZ);

                // BIOME-SPEZIFISCHE TERRAIN-GENERATION mit Blending
                switch (zoneType) {
                    case 0: // WARPED_FOREST - Normal
                        generateNormalTerrain(chunkData, x, z, absX, absZ, random,
                                floorNoise, ceilingNoise, pillarNoise, oreNoise, crystalNoise, caveNoise,
                                densityMultiplier, zoneBlend);
                        break;

                    case 1: // CRIMSON_FOREST - Geode (Schwebende Inseln)
                        generateGeodeTerrain(chunkData, x, z, absX, absZ, random,
                                floorNoise, ceilingNoise, geodeNoise, islandNoise, oreNoise, crystalNoise, caveNoise,
                                densityMultiplier);
                        break;

                    case 2: // SOUL_SAND_VALLEY - Kristall-Feld
                        generateCrystalFieldTerrain(chunkData, x, z, absX, absZ, random,
                                floorNoise, ceilingNoise, treeNoise, crystalNoise, oreNoise, caveNoise,
                                densityMultiplier, zoneBlend);
                        break;

                    case 3: // BASALT_DELTAS - Tiefe Zone
                        generateDeepZoneTerrain(chunkData, x, z, absX, absZ, random,
                                floorNoise, ceilingNoise, pillarNoise, oreNoise, crystalNoise, caveNoise,
                                densityMultiplier, zoneBlend);
                        break;
                }
            }
        }
    }

    // ==================== NORMAL ZONE (WARPED_FOREST) ====================
    private void generateNormalTerrain(ChunkData chunkData, int x, int z, int absX, int absZ, Random random,
                                       SimplexOctaveGenerator floorNoise, SimplexOctaveGenerator ceilingNoise,
                                       SimplexOctaveGenerator pillarNoise, SimplexOctaveGenerator oreNoise,
                                       SimplexOctaveGenerator crystalNoise, SimplexOctaveGenerator caveNoise,
                                       double densityMultiplier, double zoneBlend) {

        double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
        int floorHeight = (int) (floorValue * 30) + 25; // War 15-50, jetzt 25-55 (über Bedrock)

        double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
        int ceilingStart = (int) (ceilingValue * 40) + 200; // HÖHER: 200-240 (war 95-115)

        // HÖHLENSYSTEM - 3D Noise für organische Höhlen

        // Boden MIT HÖHLEN
        for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
            // Höhlen-Check (3D Noise)
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);

            // Große Höhlen im mittleren Bereich
            boolean isLargeCave = caveValue > 0.6 && y > chunkData.getMinHeight() + 10 && y < floorHeight - 5;

            // Kleine Höhlen/Adern überall
            boolean isSmallCave = caveValue > 0.75 && y > chunkData.getMinHeight() + 5;

            if (!isLargeCave && !isSmallCave) {
                Material block = selectNormalBlock(random, y, floorHeight, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // Decke MIT HÖHLEN
        for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);

            // Höhlen in der Decke
            boolean isCave = caveValue > 0.65 && y < chunkData.getMaxHeight() - 10;

            if (!isCave) {
                Material block = selectNormalBlock(random, y, ceilingStart, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // Normale Säulen - angepasst an neue Höhe
        double pillarValue = pillarNoise.noise(absX, absZ, 1, 1, true);
        if (pillarValue > 0.65) {
            Material pillarMaterial = pillarValue > 0.85 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;

            // Höhere Säulen durch größeren Raum
            int pillarTop = Math.min(ceilingStart - 5, floorHeight + 60 + random.nextInt(40));

            for (int y = floorHeight; y < pillarTop; y++) {
                if (random.nextInt(100) < 15) {
                    chunkData.setBlock(x, y, z, Material.AMETHYST_CLUSTER);
                } else {
                    chunkData.setBlock(x, y, z, pillarMaterial);
                }
            }
        }

        // Amethyst-Türme (kleine Formationen vom Boden)
        if (pillarValue < -0.75 && random.nextBoolean()) {
            int towerHeight = random.nextInt(12) + 8; // Höher
            for (int dy = 0; dy < towerHeight; dy++) {
                int currentY = floorHeight + dy;
                if (currentY < ceilingStart - 5) {
                    Material towerBlock = dy < 3 ? Material.TUFF :
                            (random.nextInt(100) < 40 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK);
                    chunkData.setBlock(x, currentY, z, towerBlock);
                }
            }
        }

        // MASSIVE Stalaktiten von hoher Decke
        double spikeValue = pillarNoise.noise(absX * 2, absZ * 2, 0.5, 0.5, true);
        if (spikeValue > 0.82) {
            int spikeHeight = random.nextInt(20) + 15; // VIEL länger
            int spikeStartY = ceilingStart - 1;

            for (int dy = 0; dy < spikeHeight; dy++) {
                int currentY = spikeStartY - dy;
                if (currentY > floorHeight + 15) {
                    if (dy < spikeHeight - 2 || random.nextBoolean()) {
                        Material spikeMaterial = dy < 3 && random.nextInt(100) < 30
                                ? Material.BUDDING_AMETHYST
                                : Material.AMETHYST_BLOCK;
                        chunkData.setBlock(x, currentY, z, spikeMaterial);

                        if (dy == spikeHeight - 1 && random.nextInt(100) < 40) {
                            chunkData.setBlock(x, currentY, z, Material.AMETHYST_CLUSTER);
                        }
                    }
                }
            }
        }

        // Stalagmiten vom Boden
        if (spikeValue < -0.82) {
            int spikeHeight = random.nextInt(18) + 12; // Höher
            int spikeStartY = floorHeight;

            for (int dy = 0; dy < spikeHeight; dy++) {
                int currentY = spikeStartY + dy;
                if (currentY < ceilingStart - 15) {
                    if (dy < spikeHeight - 2 || random.nextBoolean()) {
                        Material spikeMaterial = dy < 4 && random.nextInt(100) < 25
                                ? Material.BUDDING_AMETHYST
                                : Material.AMETHYST_BLOCK;
                        chunkData.setBlock(x, currentY, z, spikeMaterial);

                        if (dy == spikeHeight - 1 && random.nextInt(100) < 50) {
                            chunkData.setBlock(x, currentY, z, Material.AMETHYST_CLUSTER);
                        }
                    }
                }
            }
        }
    }

    // ==================== GEODE ZONE (CRIMSON_FOREST) ====================
    private void generateGeodeTerrain(ChunkData chunkData, int x, int z, int absX, int absZ, Random random,
                                      SimplexOctaveGenerator floorNoise, SimplexOctaveGenerator ceilingNoise,
                                      SimplexOctaveGenerator geodeNoise, SimplexOctaveGenerator islandNoise,
                                      SimplexOctaveGenerator oreNoise, SimplexOctaveGenerator crystalNoise,
                                      SimplexOctaveGenerator caveNoise, double densityMultiplier) {

        double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
        int floorHeight = (int) (floorValue * 15) + 20; // War 10-30, jetzt 20-35 (über Bedrock)

        double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
        int ceilingStart = (int) (ceilingValue * 50) + 210; // EXTREM hoch: 210-260

        // Minimaler Boden MIT HÖHLEN
        for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);

            // Höhlen/Adern
            boolean isCave = caveValue > 0.7 && y > chunkData.getMinHeight() + 5 && y < floorHeight - 3;

            if (!isCave) {
                Material block = selectGeodeBlock(random, y, floorHeight, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // Decke MIT HÖHLEN
        for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);
            boolean isCave = caveValue > 0.65 && y < chunkData.getMaxHeight() - 10;

            if (!isCave) {
                Material block = selectGeodeBlock(random, y, ceilingStart, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // VERBESSERTE SCHWEBENDE INSELN - 3D Noise für organische Formen
        double islandValue = islandNoise.noise(absX, absZ, 0.5, 0.5, true);
        double geodeValue = geodeNoise.noise(absX, absZ, 0.5, 0.5, true);

        // Blend-Faktor für sanfte Übergänge an Biom-Grenzen
        double zoneBlend = biomeProvider.getZoneBlend(absX, absZ);

        // Nur Inseln spawnen wenn wir tief genug in der Geode-Zone sind
        if (zoneBlend > 0.3) {
            // Mehr vertikaler Raum = höhere Inseln möglich
            double baseIslandY = 60 + (islandValue * 80); // War 50-75, jetzt 60-140

            // Große zusammenhängende Insel-Formationen
            if (islandValue > 0.4) {
                boolean isGeodeCore = geodeValue > 0.65;

                // Multi-Layer Island mit 3D-Form (dicker für mehr Eindruck)
                for (int layerOffset = -5; layerOffset <= 6; layerOffset++) {
                    int currentY = (int)baseIslandY + layerOffset;

                    if (currentY <= floorHeight + 20 || currentY >= ceilingStart - 15) {
                        continue;
                    }

                    // 3D Noise für organische Insel-Form
                    double shapeNoise = crystalNoise.noise(absX, currentY * 0.5, absZ, 0.5, 0.5, true);

                    // Insel wird zur Mitte hin dicker
                    double layerFactor = 1.0 - (Math.abs(layerOffset) / 6.0);
                    double threshold = 0.4 - (layerFactor * 0.25) + (zoneBlend * 0.1);

                    if (shapeNoise > threshold) {
                        // Hohle Geode in der Mitte großer Inseln
                        if (isGeodeCore && layerOffset >= -2 && layerOffset <= 3) {
                            double centerDist = shapeNoise;
                            if (centerDist > 0.7 && centerDist < 0.85) {
                                continue; // Hohl für Geode-Innenraum
                            }
                        }

                        Material islandBlock;

                        // Äußere Schicht (Boden/Decke)
                        if (layerOffset == -5 || layerOffset == 6) {
                            islandBlock = Material.SMOOTH_BASALT;
                        }
                        // Basis-Schichten
                        else if (layerOffset <= -2) {
                            int roll = random.nextInt(100);
                            if (roll < 30) islandBlock = Material.TUFF;
                            else if (roll < 60) islandBlock = Material.SMOOTH_BASALT;
                            else islandBlock = Material.CALCITE;
                        }
                        // Kern-Schichten (viel Amethyst)
                        else if (isGeodeCore) {
                            int roll = random.nextInt(100);
                            if (roll < 65) islandBlock = Material.BUDDING_AMETHYST;
                            else if (roll < 90) islandBlock = Material.AMETHYST_BLOCK;
                            else islandBlock = Material.CALCITE;
                        }
                        // Standard Insel-Material
                        else {
                            int roll = random.nextInt(100);
                            if (roll < 45) islandBlock = Material.AMETHYST_BLOCK;
                            else if (roll < 70) islandBlock = Material.BUDDING_AMETHYST;
                            else if (roll < 85) islandBlock = Material.TUFF;
                            else islandBlock = Material.CALCITE;
                        }

                        chunkData.setBlock(x, currentY, z, islandBlock);
                    }
                }

                // LÄNGERE hängende Kristalle von Insel-Unterseite
                if (random.nextInt(100) < (30 * zoneBlend)) {
                    int baseY = (int)baseIslandY - 5;
                    double hangNoise = geodeNoise.noise(absX * 2, absZ * 2, 0.5, 0.5, true);

                    if (hangNoise > 0.6) {
                        int hangLength = 8 + random.nextInt(15); // War 3-9, jetzt 8-23
                        for (int dy = 1; dy <= hangLength; dy++) {
                            int hangY = baseY - dy;
                            if (hangY > floorHeight + 8) {
                                // Dünner werdend
                                if (dy < hangLength - 2 || random.nextBoolean()) {
                                    Material hangBlock;
                                    if (dy == hangLength) {
                                        hangBlock = Material.AMETHYST_CLUSTER;
                                    } else if (dy > hangLength - 3) {
                                        hangBlock = Material.LARGE_AMETHYST_BUD;
                                    } else {
                                        hangBlock = random.nextBoolean() ? Material.AMETHYST_BLOCK : Material.BUDDING_AMETHYST;
                                    }
                                    chunkData.setBlock(x, hangY, z, hangBlock);
                                }
                            }
                        }
                    }
                }
            }

            // Verbindungs-Brücken zwischen Inseln - mehr vertikale Variation
            double bridgeNoise = geodeNoise.noise(absX * 1.5, absZ * 1.5, 0.5, 0.5, true);
            if (bridgeNoise > 0.55 && bridgeNoise < 0.65 && zoneBlend > 0.5) {
                int bridgeY = 70 + (int)(bridgeNoise * 80); // War 55-75, jetzt 70-150
                if (bridgeY > floorHeight + 15 && bridgeY < ceilingStart - 15) {
                    Material bridgeMaterial = random.nextInt(100) < 60 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;
                    chunkData.setBlock(x, bridgeY, z, bridgeMaterial);

                    // Cluster-Verzierung
                    if (random.nextInt(100) < 35 && bridgeY < ceilingStart - 16) {
                        chunkData.setBlock(x, bridgeY + 1, z, Material.AMETHYST_CLUSTER);
                    }
                    if (random.nextInt(100) < 25 && bridgeY > floorHeight + 16) {
                        chunkData.setBlock(x, bridgeY - 1, z, Material.LARGE_AMETHYST_BUD);
                    }
                }
            }

            // Kleine schwebende Kristall-Cluster auf mehreren Ebenen
            if (islandValue > 0.2 && islandValue < 0.4 && zoneBlend > 0.4) {
                double floatNoise = crystalNoise.noise(absX * 3, absZ * 3, 0.5, 0.5, true);
                if (floatNoise > 0.75) {
                    int floatY = 50 + (int)(floatNoise * 120); // War 45-80, jetzt 50-170
                    if (floatY > floorHeight + 12 && floatY < ceilingStart - 12) {
                        int clusterSize = 1 + random.nextInt(3);
                        for (int dy = 0; dy < clusterSize; dy++) {
                            if (floatY + dy < ceilingStart - 12) {
                                Material floatBlock = dy == clusterSize - 1 ? Material.AMETHYST_CLUSTER : Material.BUDDING_AMETHYST;
                                chunkData.setBlock(x, floatY + dy, z, floatBlock);
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== CRYSTAL FIELD (SOUL_SAND_VALLEY) ====================
    private void generateCrystalFieldTerrain(ChunkData chunkData, int x, int z, int absX, int absZ, Random random,
                                             SimplexOctaveGenerator floorNoise, SimplexOctaveGenerator ceilingNoise,
                                             SimplexOctaveGenerator treeNoise, SimplexOctaveGenerator crystalNoise,
                                             SimplexOctaveGenerator oreNoise, SimplexOctaveGenerator caveNoise,
                                             double densityMultiplier, double zoneBlend) {

        double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
        int floorHeight = (int) (floorValue * 10) + 35; // War 30-42, jetzt 35-45 (sicher über Bedrock)

        double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
        int ceilingStart = (int) (ceilingValue * 50) + 220; // SEHR hoch: 220-270

        // Flacher Kristall-Boden MIT HÖHLEN
        for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);
            boolean isCave = caveValue > 0.72 && y > chunkData.getMinHeight() + 5;

            if (!isCave) {
                Material block = selectCrystalFieldBlock(random, y, floorHeight, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // Decke MIT HÖHLEN
        for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);
            boolean isCave = caveValue > 0.65 && y < chunkData.getMaxHeight() - 10;

            if (!isCave) {
                Material block = selectCrystalFieldBlock(random, y, ceilingStart, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // RIESIGE KRISTALL-BÄUME - viel höher durch mehr Raum
        double treeValue = treeNoise.noise(absX, absZ, 0.5, 0.5, true);
        if (treeValue > (0.7 - zoneBlend * 0.1) && zoneBlend > 0.4) {
            int treeHeight = 25 + random.nextInt((int)(40 * zoneBlend)); // War 12-22, jetzt 25-65

            for (int dy = 0; dy < treeHeight; dy++) {
                int currentY = floorHeight + dy;
                if (currentY < ceilingStart - 10) {

                    Material trunkBlock;
                    if (dy < 6) {
                        trunkBlock = Material.TUFF; // Basis
                    } else if (dy < treeHeight - 6) {
                        trunkBlock = Material.BUDDING_AMETHYST; // Stamm
                    } else {
                        trunkBlock = Material.AMETHYST_CLUSTER; // Krone
                    }

                    chunkData.setBlock(x, currentY, z, trunkBlock);

                    // Mehr Äste bei höheren Bäumen
                    if (dy > 10 && dy < treeHeight - 3 && random.nextInt(100) < (40 * zoneBlend)) {
                        int branchOffset = random.nextBoolean() ? 1 : -1;
                        if (x + branchOffset >= 0 && x + branchOffset < 16) {
                            chunkData.setBlock(x + branchOffset, currentY, z, Material.AMETHYST_CLUSTER);
                        }
                    }
                }
            }
        }

        // HÖHERE Kristall-Geysire
        double geysirValue = crystalNoise.noise(absX * 2, absZ * 2, 0.5, 0.5, true);
        if (geysirValue > (0.8 - zoneBlend * 0.15) && zoneBlend > 0.3) {
            int geysirHeight = 12 + random.nextInt((int)(20 * zoneBlend)); // War 6-14, jetzt 12-32
            for (int dy = 0; dy < geysirHeight; dy++) {
                int currentY = floorHeight + dy;
                if (currentY < ceilingStart - 10) {
                    Material geysirBlock;
                    if (dy % 2 == 0) {
                        geysirBlock = Material.AMETHYST_CLUSTER;
                    } else {
                        geysirBlock = Material.BUDDING_AMETHYST;
                    }
                    chunkData.setBlock(x, currentY, z, geysirBlock);
                }
            }
        }

        // Höhere Kristalline Bögen
        if (treeValue < -0.75 && random.nextInt(100) < (25 * zoneBlend)) {
            int archHeight = 10 + random.nextInt(12); // War 5-9, jetzt 10-22
            int archStart = floorHeight;

            for (int dy = 0; dy < archHeight; dy++) {
                int currentY = archStart + dy;
                if (currentY < ceilingStart - 10) {
                    Material archBlock = dy < 3 ? Material.SMOOTH_BASALT :
                            (dy == archHeight - 1 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK);
                    chunkData.setBlock(x, currentY, z, archBlock);
                }
            }
        }

        // Massive hängende Kristall-Formationen von hoher Decke
        if (geysirValue > 0.75 && zoneBlend > 0.5) {
            int hangHeight = 15 + random.nextInt(25); // Lange Stalaktiten
            int startY = ceilingStart - 1;

            for (int dy = 0; dy < hangHeight; dy++) {
                int currentY = startY - dy;
                if (currentY > floorHeight + 20) {
                    if (dy < hangHeight - 2 || random.nextBoolean()) {
                        Material hangBlock;
                        if (dy == hangHeight - 1) {
                            hangBlock = Material.AMETHYST_CLUSTER;
                        } else if (dy > hangHeight - 4) {
                            hangBlock = Material.LARGE_AMETHYST_BUD;
                        } else {
                            hangBlock = Material.BUDDING_AMETHYST;
                        }
                        chunkData.setBlock(x, currentY, z, hangBlock);
                    }
                }
            }
        }
    }

    // ==================== DEEP ZONE (BASALT_DELTAS) ====================
    private void generateDeepZoneTerrain(ChunkData chunkData, int x, int z, int absX, int absZ, Random random,
                                         SimplexOctaveGenerator floorNoise, SimplexOctaveGenerator ceilingNoise,
                                         SimplexOctaveGenerator pillarNoise, SimplexOctaveGenerator oreNoise,
                                         SimplexOctaveGenerator crystalNoise, SimplexOctaveGenerator caveNoise,
                                         double densityMultiplier, double zoneBlend) {

        double floorValue = floorNoise.noise(absX, absZ, 0.5, 0.5, true);
        int floorHeight = (int) (floorValue * 40) + 30; // War 20-65, jetzt 30-70 (über Bedrock)

        double ceilingValue = ceilingNoise.noise(absX, absZ, 0.5, 0.5, true);
        int ceilingStart = (int) (ceilingValue * 60) + 190; // Hoch und bedrohlich: 190-250

        // Massiver Boden MIT GROSSEN HÖHLEN
        for (int y = chunkData.getMinHeight(); y < floorHeight; y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);

            // GROSSE unterirdische Höhlen
            boolean isLargeCave = caveValue > 0.55 && y > chunkData.getMinHeight() + 15 && y < floorHeight - 8;

            // Kleinere Höhlen
            boolean isSmallCave = caveValue > 0.7 && y > chunkData.getMinHeight() + 8;

            if (!isLargeCave && !isSmallCave) {
                Material block = selectDeepZoneBlock(random, y, floorHeight, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // Massive Decke MIT HÖHLEN
        for (int y = ceilingStart; y < chunkData.getMaxHeight(); y++) {
            double caveValue = caveNoise.noise(absX, y, absZ, 0.5, 0.5, true);
            boolean isCave = caveValue > 0.6 && y < chunkData.getMaxHeight() - 10;

            if (!isCave) {
                Material block = selectDeepZoneBlock(random, y, ceilingStart, absX, y, absZ,
                        oreNoise, crystalNoise, densityMultiplier);
                chunkData.setBlock(x, y, z, block);
            }
        }

        // MASSIVE Obsidian-Spitzen - nur im Zonen-Zentrum
        double pillarValue = pillarNoise.noise(absX, absZ, 1, 1, true);
        if (pillarValue > (0.88 - zoneBlend * 0.05) && zoneBlend > 0.5) {
            int spikeHeight = 15 + random.nextInt((int)(30 * zoneBlend)); // War 8-18, jetzt 15-45
            for (int dy = 0; dy < spikeHeight; dy++) {
                int currentY = floorHeight + dy;
                if (currentY < ceilingStart - 15) {
                    Material spikeBlock;
                    if (dy < 5) {
                        spikeBlock = Material.BLACKSTONE;
                    } else if (dy < spikeHeight - 4) {
                        spikeBlock = random.nextInt(100) < 30 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    } else {
                        spikeBlock = Material.OBSIDIAN;
                    }
                    chunkData.setBlock(x, currentY, z, spikeBlock);
                }
            }
        }

        // Höhere Basalt-Säulen
        if (pillarValue > 0.70 && pillarValue <= 0.88 && zoneBlend > 0.3) {
            int basaltHeight = 10 + random.nextInt((int)(20 * zoneBlend)); // War 6-14, jetzt 10-30
            for (int dy = 0; dy < basaltHeight; dy++) {
                int currentY = floorHeight + dy;
                if (currentY < ceilingStart - 10) {
                    Material basaltBlock = random.nextBoolean() ? Material.BASALT : Material.SMOOTH_BASALT;
                    chunkData.setBlock(x, currentY, z, basaltBlock);
                }
            }
        }

        // Versteckte Amethyst-Adern IN HÖHLEN (selten aber wertvoll)
        double crystalValue = crystalNoise.noise(absX, absZ, 0.5, 0.5, true);
        if (crystalValue > 0.85 && random.nextInt(100) < (20 * zoneBlend)) {
            int veinY = floorHeight + random.nextInt((ceilingStart - floorHeight) / 2);
            if (veinY < ceilingStart - 10) {
                chunkData.setBlock(x, veinY, z, Material.BUDDING_AMETHYST);

                // Größere Cluster-Gruppe in Höhlen
                if (random.nextInt(100) < 60) {
                    chunkData.setBlock(x, veinY + 1, z, Material.AMETHYST_CLUSTER);
                }
                if (random.nextInt(100) < 40 && veinY > floorHeight + 5) {
                    chunkData.setBlock(x, veinY - 1, z, Material.AMETHYST_BLOCK);
                }
            }
        }

        // Massive Basalt-Stalaktiten von hoher Decke
        if (pillarValue < -0.82 && zoneBlend > 0.4) {
            int stalactiteHeight = 12 + random.nextInt(20); // Lange Stalaktiten
            int startY = ceilingStart - 1;

            for (int dy = 0; dy < stalactiteHeight; dy++) {
                int currentY = startY - dy;
                if (currentY > floorHeight + 20) {
                    if (dy < stalactiteHeight - 2 || random.nextBoolean()) {
                        Material stalactiteBlock;
                        if (dy < 3) {
                            stalactiteBlock = Material.BLACKSTONE;
                        } else if (dy < stalactiteHeight - 3) {
                            stalactiteBlock = Material.BASALT;
                        } else {
                            stalactiteBlock = Material.SMOOTH_BASALT;
                        }
                        chunkData.setBlock(x, currentY, z, stalactiteBlock);
                    }
                }
            }
        }
    }

    // ==================== BLOCK SELECTION METHODS ====================

    private Material selectNormalBlock(Random random, int y, int surfaceLevel,
                                       int worldX, int worldY, int worldZ,
                                       SimplexOctaveGenerator oreNoise,
                                       SimplexOctaveGenerator crystalNoise,
                                       double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        // Oraxen Ores
        if (oraxenIntegration != null && depth > 3 && depth < 20) {
            double oreValue = oreNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);
            if (depth <= 8 && oreValue > 0.92) {
                Material ore = oraxenIntegration.getRandomOre(random, "common");
                if (ore != null) return ore;
            }
        }

        if (depth <= 1) {
            return random.nextInt(100) < 45 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;
        }

        if (depth <= 5) {
            int roll = random.nextInt(100);
            if (roll < 55) return Material.AMETHYST_BLOCK;
            if (roll < 75) return Material.BUDDING_AMETHYST;
            if (roll < 85) return Material.TUFF;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 15) {
            int roll = random.nextInt(100);
            if (roll < 40) return Material.AMETHYST_BLOCK;
            if (roll < 60) return Material.TUFF;
            if (roll < 80) return Material.SMOOTH_BASALT;
            return Material.DEEPSLATE;
        }

        int roll = random.nextInt(100);
        if (roll < 25) return Material.TUFF;
        if (roll < 50) return Material.DEEPSLATE;
        if (roll < 75) return Material.BLACKSTONE;
        return Material.SMOOTH_BASALT;
    }

    private Material selectGeodeBlock(Random random, int y, int surfaceLevel,
                                      int worldX, int worldY, int worldZ,
                                      SimplexOctaveGenerator oreNoise,
                                      SimplexOctaveGenerator crystalNoise,
                                      double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        // Fast nur Amethyst
        if (depth <= 1) {
            return random.nextInt(100) < 60 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;
        }

        if (depth <= 10) {
            int roll = random.nextInt(100);
            if (roll < 75) return Material.AMETHYST_BLOCK;
            if (roll < 90) return Material.BUDDING_AMETHYST;
            return Material.SMOOTH_BASALT;
        }

        int roll = random.nextInt(100);
        if (roll < 50) return Material.AMETHYST_BLOCK;
        if (roll < 70) return Material.SMOOTH_BASALT;
        if (roll < 85) return Material.TUFF;
        return Material.CALCITE;
    }

    private Material selectCrystalFieldBlock(Random random, int y, int surfaceLevel,
                                             int worldX, int worldY, int worldZ,
                                             SimplexOctaveGenerator oreNoise,
                                             SimplexOctaveGenerator crystalNoise,
                                             double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        if (depth <= 1) {
            return random.nextInt(100) < 70 ? Material.BUDDING_AMETHYST : Material.AMETHYST_CLUSTER;
        }

        if (depth <= 8) {
            int roll = random.nextInt(100);
            if (roll < 60) return Material.BUDDING_AMETHYST;
            if (roll < 85) return Material.AMETHYST_BLOCK;
            return Material.TUFF;
        }

        int roll = random.nextInt(100);
        if (roll < 45) return Material.AMETHYST_BLOCK;
        if (roll < 70) return Material.TUFF;
        if (roll < 90) return Material.CALCITE;
        return Material.SMOOTH_BASALT;
    }

    private Material selectDeepZoneBlock(Random random, int y, int surfaceLevel,
                                         int worldX, int worldY, int worldZ,
                                         SimplexOctaveGenerator oreNoise,
                                         SimplexOctaveGenerator crystalNoise,
                                         double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        // Oraxen Ores (seltener)
        if (oraxenIntegration != null && depth > 5 && depth < 15) {
            double oreValue = oreNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);
            if (oreValue > 0.94) {
                Material ore = oraxenIntegration.getRandomOre(random, "rare");
                if (ore != null) return ore;
            }
        }

        if (depth <= 1) {
            int roll = random.nextInt(100);
            if (roll < 20) return Material.AMETHYST_BLOCK;
            if (roll < 40) return Material.BLACKSTONE;
            if (roll < 70) return Material.BASALT;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 8) {
            int roll = random.nextInt(100);
            if (roll < 15) return Material.AMETHYST_BLOCK;
            if (roll < 35) return Material.BLACKSTONE;
            if (roll < 60) return Material.BASALT;
            if (roll < 80) return Material.DEEPSLATE;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 20) {
            int roll = random.nextInt(100);
            if (roll < 10) return Material.AMETHYST_BLOCK;
            if (roll < 30) return Material.BLACKSTONE;
            if (roll < 55) return Material.DEEPSLATE;
            if (roll < 75) return Material.BASALT;
            return Material.TUFF;
        }

        int roll = random.nextInt(100);
        if (roll < 35) return Material.DEEPSLATE;
        if (roll < 60) return Material.BLACKSTONE;
        if (roll < 80) return Material.BASALT;
        return Material.TUFF;
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        if (biomeProvider == null) {
            biomeProvider = new AmethystBiomeProvider(worldInfo.getSeed());
        }

        SimplexOctaveGenerator clusterNoise = getNoiseGenerator(worldInfo.getSeed() + 8000, 0.04);
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = worldX + x;
                int absZ = worldZ + z;

                int zoneType = biomeProvider.getZoneType(absX, absZ);
                double clusterDensity = clusterNoise.noise(absX, absZ, 0.5, 0.5, true);
                boolean highDensity = clusterDensity > 0.6 || zoneType == 2;

                boolean placedFloorCluster = false;
                boolean placedCeilingCluster = false;

                // Boden-Oberfläche - Cluster wachsen NACH OBEN
                for (int y = 80; y > chunkData.getMinHeight(); y--) {
                    Material currentBlock = chunkData.getType(x, y, z);
                    Material blockAbove = chunkData.getType(x, y + 1, z);

                    // Finde erste Oberfläche (solid block mit Luft drüber)
                    if (currentBlock != Material.AIR && blockAbove == Material.AIR && !placedFloorCluster) {
                        int clusterChance = getClusterChance(zoneType, highDensity);

                        if (random.nextInt(100) < clusterChance && y + 1 < chunkData.getMaxHeight()) {
                            placeAmethystCluster(chunkData, x, y + 1, z, random, highDensity || zoneType == 2, true);
                            placedFloorCluster = true;
                        }

                        // Calcite nur in Normal/Geode
                        if (!placedFloorCluster && (zoneType == 0 || zoneType == 1) && random.nextInt(100) < 2) {
                            chunkData.setBlock(x, y + 1, z, Material.CALCITE);
                            placedFloorCluster = true;
                        }
                        break;
                    }
                }

                // Decken-Oberfläche - Cluster wachsen NACH UNTEN
                for (int y = 90; y < chunkData.getMaxHeight(); y++) {
                    Material currentBlock = chunkData.getType(x, y, z);
                    Material blockBelow = chunkData.getType(x, y - 1, z);

                    // Finde erste Decke (solid block mit Luft drunter)
                    if (currentBlock != Material.AIR && blockBelow == Material.AIR && !placedCeilingCluster) {
                        int hangingChance = getHangingClusterChance(zoneType);

                        if (random.nextInt(100) < hangingChance && y - 1 > chunkData.getMinHeight()) {
                            placeAmethystCluster(chunkData, x, y - 1, z, random, highDensity, false);
                            placedCeilingCluster = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    private int getClusterChance(int zoneType, boolean highDensity) {
        switch (zoneType) {
            case 1: return 50; // Geode
            case 2: return 70; // Kristall-Feld
            case 3: return 10; // Tiefe Zone
            default: return highDensity ? 40 : 25; // Normal
        }
    }

    private int getHangingClusterChance(int zoneType) {
        switch (zoneType) {
            case 1: return 55; // Geode
            case 2: return 45; // Kristall-Feld
            case 3: return 8;  // Tiefe Zone
            default: return 30; // Normal
        }
    }

    private void placeAmethystCluster(ChunkData data, int x, int y, int z, Random random, boolean large, boolean upward) {
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
            // Prüfe ob Position frei ist
            if (data.getType(x, y, z) != Material.AIR) {
                return; // Verhindere Stacking
            }

            Material[] clusters = large ? largeClusters : smallClusters;
            Material clusterMaterial = clusters[random.nextInt(clusters.length)];

            // Setze Cluster mit korrekter Orientierung
            data.setBlock(x, y, z, clusterMaterial);

            // Optional: BlockData für Orientierung setzen (nach oben/unten)
            // Minecraft-Cluster orientieren sich automatisch zur attachten Fläche
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
    @Override public boolean shouldGenerateStructures() { return true; }
    @Override public boolean shouldGenerateMobs() { return true; }
}