package de.tecca.ultimatedimensions.dimension;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public abstract class BaseBiomeProvider extends BiomeProvider {

    protected final long seed;
    protected final SimplexOctaveGenerator biomeNoise;
    protected final SimplexOctaveGenerator detailNoise;

    public BaseBiomeProvider(long seed) {
        this.seed = seed;
        this.biomeNoise = new SimplexOctaveGenerator(new Random(seed), 5);
        this.biomeNoise.setScale(0.0015);

        this.detailNoise = new SimplexOctaveGenerator(new Random(seed + 1000), 4);
        this.detailNoise.setScale(0.008);
    }

    @NotNull
    @Override
    public abstract Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z);

    @NotNull
    @Override
    public abstract List<Biome> getBiomes(@NotNull WorldInfo worldInfo);

    public abstract int getZoneType(int x, int z);

    public abstract double getZoneBlend(int x, int z);

    public abstract String getZoneName(int x, int z);

    protected double getCombinedNoise(int x, int z) {
        double biomeValue = biomeNoise.noise(x, z, 0.5, 0.5, true);
        double detailValue = detailNoise.noise(x, z, 0.5, 0.5, true);
        return biomeValue + detailValue * 0.2;
    }
}