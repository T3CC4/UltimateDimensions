package de.tecca.ultimatedimensions.generator;

import de.tecca.ultimatedimensions.util.OraxenIntegration;
import org.bukkit.Material;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class BlockSelector {

    private final TerrainConstants constants;
    private final OraxenIntegration oraxenIntegration;

    public BlockSelector(TerrainConstants constants, OraxenIntegration oraxenIntegration) {
        this.constants = constants;
        this.oraxenIntegration = oraxenIntegration;
    }

    public Material selectNormalBlock(Random random, int y, int surfaceLevel,
                                      int worldX, int worldY, int worldZ,
                                      SimplexOctaveGenerator oreNoise,
                                      SimplexOctaveGenerator crystalNoise,
                                      double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        if (oraxenIntegration != null &&
                depth > constants.oraxen.normalOreMinDepth &&
                depth < constants.oraxen.normalOreMaxDepth) {

            double oreValue = oreNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);
            if (depth <= constants.oraxen.normalOreDepthCheckMax &&
                    oreValue > constants.oraxen.commonOreThreshold) {
                Material ore = oraxenIntegration.getRandomOre(random, "common");
                if (ore != null) return ore;
            }
        }

        if (depth <= 1) {
            return random.nextInt(100) < constants.blocks.normalSurfaceBuddingChance
                    ? Material.BUDDING_AMETHYST
                    : Material.AMETHYST_BLOCK;
        }

        if (depth <= 5) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.normalUpperAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.normalUpperBuddingChance) return Material.BUDDING_AMETHYST;
            if (roll < constants.blocks.normalUpperTuffChance) return Material.TUFF;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 15) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.normalMiddleAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.normalMiddleTuffChance) return Material.TUFF;
            if (roll < constants.blocks.normalMiddleBasaltChance) return Material.SMOOTH_BASALT;
            return Material.DEEPSLATE;
        }

        int roll = random.nextInt(100);
        if (roll < constants.blocks.normalDeepTuffChance) return Material.TUFF;
        if (roll < constants.blocks.normalDeepDeepslateChance) return Material.DEEPSLATE;
        if (roll < constants.blocks.normalDeepBlackstoneChance) return Material.BLACKSTONE;
        return Material.SMOOTH_BASALT;
    }

    public Material selectGeodeBlock(Random random, int y, int surfaceLevel,
                                     int worldX, int worldY, int worldZ,
                                     SimplexOctaveGenerator oreNoise,
                                     SimplexOctaveGenerator crystalNoise,
                                     double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        if (depth <= 1) {
            return random.nextInt(100) < constants.blocks.geodeSurfaceBuddingChance
                    ? Material.BUDDING_AMETHYST
                    : Material.AMETHYST_BLOCK;
        }

        if (depth <= 10) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.geodeUpperAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.geodeUpperBuddingChance) return Material.BUDDING_AMETHYST;
            return Material.SMOOTH_BASALT;
        }

        int roll = random.nextInt(100);
        if (roll < constants.blocks.geodeDeepAmethystChance) return Material.AMETHYST_BLOCK;
        if (roll < constants.blocks.geodeDeepBasaltChance) return Material.SMOOTH_BASALT;
        if (roll < constants.blocks.geodeDeepTuffChance) return Material.TUFF;
        return Material.CALCITE;
    }

    public Material selectCrystalFieldBlock(Random random, int y, int surfaceLevel,
                                            int worldX, int worldY, int worldZ,
                                            SimplexOctaveGenerator oreNoise,
                                            SimplexOctaveGenerator crystalNoise,
                                            double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        if (depth <= 1) {
            return random.nextInt(100) < constants.blocks.crystalSurfaceBuddingChance
                    ? Material.BUDDING_AMETHYST
                    : Material.AMETHYST_CLUSTER;
        }

        if (depth <= 8) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.crystalUpperBuddingChance) return Material.BUDDING_AMETHYST;
            if (roll < constants.blocks.crystalUpperAmethystChance) return Material.AMETHYST_BLOCK;
            return Material.TUFF;
        }

        int roll = random.nextInt(100);
        if (roll < constants.blocks.crystalDeepAmethystChance) return Material.AMETHYST_BLOCK;
        if (roll < constants.blocks.crystalDeepTuffChance) return Material.TUFF;
        if (roll < constants.blocks.crystalDeepCalciteChance) return Material.CALCITE;
        return Material.SMOOTH_BASALT;
    }

    public Material selectDeepZoneBlock(Random random, int y, int surfaceLevel,
                                        int worldX, int worldY, int worldZ,
                                        SimplexOctaveGenerator oreNoise,
                                        SimplexOctaveGenerator crystalNoise,
                                        double densityMultiplier) {

        int depth = Math.abs(y - surfaceLevel);

        if (oraxenIntegration != null &&
                depth > constants.oraxen.deepOreMinDepth &&
                depth < constants.oraxen.deepOreMaxDepth) {

            double oreValue = oreNoise.noise(worldX, worldY, worldZ, 0.5, 0.5, true);
            if (oreValue > constants.oraxen.rareOreThreshold) {
                Material ore = oraxenIntegration.getRandomOre(random, "rare");
                if (ore != null) return ore;
            }
        }

        if (depth <= 1) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.deepSurfaceAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.deepSurfaceBlackstoneChance) return Material.BLACKSTONE;
            if (roll < constants.blocks.deepSurfaceBasaltChance) return Material.BASALT;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 8) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.deepUpperAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.deepUpperBlackstoneChance) return Material.BLACKSTONE;
            if (roll < constants.blocks.deepUpperBasaltChance) return Material.BASALT;
            if (roll < constants.blocks.deepUpperDeepslateChance) return Material.DEEPSLATE;
            return Material.SMOOTH_BASALT;
        }

        if (depth <= 20) {
            int roll = random.nextInt(100);
            if (roll < constants.blocks.deepMiddleAmethystChance) return Material.AMETHYST_BLOCK;
            if (roll < constants.blocks.deepMiddleBlackstoneChance) return Material.BLACKSTONE;
            if (roll < constants.blocks.deepMiddleDeepslateChance) return Material.DEEPSLATE;
            if (roll < constants.blocks.deepMiddleBasaltChance) return Material.BASALT;
            return Material.TUFF;
        }

        int roll = random.nextInt(100);
        if (roll < constants.blocks.deepDeepDeepslateChance) return Material.DEEPSLATE;
        if (roll < constants.blocks.deepDeepBlackstoneChance) return Material.BLACKSTONE;
        if (roll < constants.blocks.deepDeepBasaltChance) return Material.BASALT;
        return Material.TUFF;
    }
}