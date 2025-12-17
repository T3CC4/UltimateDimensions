package de.tecca.ultimatedimensions.generator;

import org.bukkit.configuration.file.FileConfiguration;

public class TerrainConstants {

    public static class Normal {
        public int floorBaseHeight = 25;
        public int floorVariation = 30;
        public int ceilingBase = 200;
        public int ceilingVariation = 40;

        public double pillarThreshold = 0.65;
        public double elitePillarThreshold = 0.85;
        public double towerThreshold = -0.75;
        public double spikeThreshold = 0.82;
        public double stalagmiteThreshold = -0.82;

        public int minPillarHeight = 60;
        public int maxPillarHeight = 100;
        public int minTowerHeight = 8;
        public int maxTowerHeight = 20;
        public int minSpikeHeight = 15;
        public int maxSpikeHeight = 35;
        public int minStalagmiteHeight = 12;
        public int maxStalagmiteHeight = 30;

        public int clusterChanceBase = 25;
        public int clusterChanceHigh = 40;
        public int hangingClusterChance = 30;
    }

    public static class Geode {
        public int floorBaseHeight = 20;
        public int floorVariation = 15;
        public int ceilingBase = 210;
        public int ceilingVariation = 50;

        public double islandThreshold = 0.4;
        public double geodeCoreThreshold = 0.65;
        public double bridgeThresholdMin = 0.55;
        public double bridgeThresholdMax = 0.65;
        public double floatThreshold = 0.75;

        public int islandBaseY = 60;
        public int islandYVariation = 80;
        public int islandLayerMin = -5;
        public int islandLayerMax = 6;

        public int minHangLength = 8;
        public int maxHangLength = 23;
        public int bridgeYBase = 70;
        public int bridgeYVariation = 80;

        public int clusterChance = 50;
        public int hangingClusterChance = 55;
    }

    public static class Crystal {
        public int floorBaseHeight = 35;
        public int floorVariation = 10;
        public int ceilingBase = 220;
        public int ceilingVariation = 50;

        public double treeThreshold = 0.7;
        public double geysirThreshold = 0.8;
        public double archThreshold = -0.75;
        public double hangThreshold = 0.75;

        public int minTreeHeight = 25;
        public int maxTreeHeight = 65;
        public int minGeysirHeight = 12;
        public int maxGeysirHeight = 32;
        public int minArchHeight = 10;
        public int maxArchHeight = 22;
        public int minHangHeight = 15;
        public int maxHangHeight = 40;

        public int clusterChance = 70;
        public int hangingClusterChance = 45;
    }

    public static class Deep {
        public int floorBaseHeight = 30;
        public int floorVariation = 40;
        public int ceilingBase = 190;
        public int ceilingVariation = 60;

        public double obsidianSpikeThreshold = 0.88;
        public double basaltPillarThresholdMin = 0.70;
        public double basaltPillarThresholdMax = 0.88;
        public double crystalVeinThreshold = 0.85;
        public double stalactiteThreshold = -0.82;

        public int minObsidianSpikeHeight = 15;
        public int maxObsidianSpikeHeight = 45;
        public int minBasaltPillarHeight = 10;
        public int maxBasaltPillarHeight = 30;
        public int minStalactiteHeight = 12;
        public int maxStalactiteHeight = 32;

        public int clusterChance = 10;
        public int hangingClusterChance = 8;
        public int crystalVeinChance = 20;
    }

    public static class Caves {
        public double largeCaveThreshold = 0.6;
        public double smallCaveThreshold = 0.75;
        public double geodeCaveThreshold = 0.7;
        public double crystalCaveThreshold = 0.72;
        public double deepLargeCaveThreshold = 0.55;
        public double deepSmallCaveThreshold = 0.7;
        public double ceilingCaveThreshold = 0.65;

        public int minDepthForLargeCaves = 10;
        public int minDepthForSmallCaves = 5;
    }

    public static class BlockDistribution {
        public int normalSurfaceBuddingChance = 45;
        public int normalUpperAmethystChance = 55;
        public int normalUpperBuddingChance = 75;
        public int normalUpperTuffChance = 85;
        public int normalMiddleAmethystChance = 40;
        public int normalMiddleTuffChance = 60;
        public int normalMiddleBasaltChance = 80;
        public int normalDeepTuffChance = 25;
        public int normalDeepDeepslateChance = 50;
        public int normalDeepBlackstoneChance = 75;

        public int geodeSurfaceBuddingChance = 60;
        public int geodeUpperAmethystChance = 75;
        public int geodeUpperBuddingChance = 90;
        public int geodeDeepAmethystChance = 50;
        public int geodeDeepBasaltChance = 70;
        public int geodeDeepTuffChance = 85;

        public int crystalSurfaceBuddingChance = 70;
        public int crystalUpperBuddingChance = 60;
        public int crystalUpperAmethystChance = 85;
        public int crystalDeepAmethystChance = 45;
        public int crystalDeepTuffChance = 70;
        public int crystalDeepCalciteChance = 90;

        public int deepSurfaceAmethystChance = 20;
        public int deepSurfaceBlackstoneChance = 40;
        public int deepSurfaceBasaltChance = 70;
        public int deepUpperAmethystChance = 15;
        public int deepUpperBlackstoneChance = 35;
        public int deepUpperBasaltChance = 60;
        public int deepUpperDeepslateChance = 80;
        public int deepMiddleAmethystChance = 10;
        public int deepMiddleBlackstoneChance = 30;
        public int deepMiddleDeepslateChance = 55;
        public int deepMiddleBasaltChance = 75;
        public int deepDeepDeepslateChance = 35;
        public int deepDeepBlackstoneChance = 60;
        public int deepDeepBasaltChance = 80;
    }

    public static class Oraxen {
        public double commonOreThreshold = 0.92;
        public double rareOreThreshold = 0.94;
        public int normalOreMinDepth = 3;
        public int normalOreMaxDepth = 20;
        public int normalOreDepthCheckMax = 8;
        public int deepOreMinDepth = 5;
        public int deepOreMaxDepth = 15;
        public long retryDelayTicks = 100L;
    }

    public static class NoiseScales {
        public double floor = 0.015;
        public double ceiling = 0.012;
        public double pillar = 0.05;
        public double ore = 0.08;
        public double crystal = 0.03;
        public double geode = 0.02;
        public double island = 0.025;
        public double tree = 0.04;
        public double cave = 0.04;
        public double cluster = 0.04;
    }

    public final Normal normal = new Normal();
    public final Geode geode = new Geode();
    public final Crystal crystal = new Crystal();
    public final Deep deep = new Deep();
    public final Caves caves = new Caves();
    public final BlockDistribution blocks = new BlockDistribution();
    public final Oraxen oraxen = new Oraxen();
    public final NoiseScales noise = new NoiseScales();

    public void loadFromConfig(FileConfiguration config) {
        if (config.contains("terrain.normal.floor_base_height")) {
            normal.floorBaseHeight = config.getInt("terrain.normal.floor_base_height");
        }
    }

    public static TerrainConstants createDefault() {
        return new TerrainConstants();
    }
}