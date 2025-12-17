package de.tecca.ultimatedimensions.generator;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Custom BiomeProvider für Amethyst-Dimension
 *
 * Generiert 4 unterschiedliche Custom-Biomes basierend auf Terrain-Zonen:
 * - WARPED_FOREST = Normal-Zone (Standard)
 * - CRIMSON_FOREST = Geode-Zone (schwebende Inseln)
 * - SOUL_SAND_VALLEY = Kristall-Feld (viele Cluster)
 * - BASALT_DELTAS = Tiefe Zone (mehr Gestein)
 */
public class AmethystBiomeProvider extends BiomeProvider {

    private final SimplexOctaveGenerator biomeNoise;
    private final SimplexOctaveGenerator detailNoise;

    // Custom-Biomes für visuelle Unterschiede
    private static final Biome BIOME_NORMAL = Biome.WARPED_FOREST;      // Zone 0
    private static final Biome BIOME_GEODE = Biome.CRIMSON_FOREST;      // Zone 1
    private static final Biome BIOME_CRYSTAL = Biome.SOUL_SAND_VALLEY;  // Zone 2
    private static final Biome BIOME_DEEP = Biome.BASALT_DELTAS;        // Zone 3

    public AmethystBiomeProvider(long seed) {
        this.biomeNoise = new SimplexOctaveGenerator(new Random(seed), 5);
        this.biomeNoise.setScale(0.0015); // SEHR große Biom-Regionen

        this.detailNoise = new SimplexOctaveGenerator(new Random(seed + 1000), 4);
        this.detailNoise.setScale(0.008); // Viel sanftere Details
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        // Biome basierend auf Zone-Type zurückgeben
        int zoneType = getZoneType(x, z);

        switch (zoneType) {
            case 1: return BIOME_GEODE;
            case 2: return BIOME_CRYSTAL;
            case 3: return BIOME_DEEP;
            default: return BIOME_NORMAL;
        }
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        // Alle 4 Custom-Biomes verfügbar machen
        return Arrays.asList(BIOME_NORMAL, BIOME_GEODE, BIOME_CRYSTAL, BIOME_DEEP);
    }

    /**
     * Berechnet Zone-Type basierend auf Noise mit sanften Übergängen
     * 0 = Normal, 1 = Geode, 2 = Crystal, 3 = Deep
     */
    public int getZoneType(int x, int z) {
        double biomeValue = biomeNoise.noise(x, z, 0.5, 0.5, true);
        double detailValue = detailNoise.noise(x, z, 0.5, 0.5, true);
        double combined = biomeValue + detailValue * 0.2; // Minimal Detail-Einfluss

        // Klar getrennte, große Zonen
        if (combined < -0.5) {
            return 2; // Crystal (25%)
        } else if (combined >= -0.1 && combined < 0.35) {
            return 1; // Geode (25%)
        } else if (combined >= 0.55) {
            return 3; // Deep (10%)
        } else {
            return 0; // Normal (40%)
        }
    }

    /**
     * Gibt Blend-Faktor zurück (0.0 - 1.0) für sanfte Übergänge
     * Je näher an 1.0, desto stärker die Zone
     */
    public double getZoneBlend(int x, int z) {
        double biomeValue = biomeNoise.noise(x, z, 0.5, 0.5, true);
        double detailValue = detailNoise.noise(x, z, 0.5, 0.5, true);
        double combined = biomeValue + detailValue * 0.2;

        int zoneType = getZoneType(x, z);

        // Berechne Distanz zum Zonen-Zentrum
        double distance;
        switch (zoneType) {
            case 0: // Normal (-0.1 bis 0.55, Zentrum ~0.2)
                distance = Math.abs(combined - 0.2);
                return Math.max(0.0, 1.0 - (distance / 0.3));
            case 1: // Geode (-0.1 bis 0.35, Zentrum ~0.12)
                distance = Math.abs(combined - 0.12);
                return Math.max(0.0, 1.0 - (distance / 0.23));
            case 2: // Crystal (< -0.5, Zentrum ~-0.7)
                distance = Math.abs(combined + 0.7);
                return Math.max(0.0, 1.0 - (distance / 0.2));
            case 3: // Deep (>= 0.55, Zentrum ~0.7)
                distance = Math.abs(combined - 0.7);
                return Math.max(0.0, 1.0 - (distance / 0.15));
            default:
                return 0.5;
        }
    }

    /**
     * Prüft ob an dieser Position eine Geode-Zone ist
     */
    public boolean isGeodeZone(int x, int z) {
        return getZoneType(x, z) == 1;
    }

    /**
     * Prüft ob an dieser Position ein Kristall-Feld ist
     */
    public boolean isCrystalField(int x, int z) {
        return getZoneType(x, z) == 2;
    }

    /**
     * Gibt Amethyst-Dichte-Multiplikator zurück
     */
    public double getAmethystDensityMultiplier(int x, int z) {
        int zone = getZoneType(x, z);
        switch (zone) {
            case 1: return 1.8; // Geode
            case 2: return 1.4; // Crystal
            default: return 1.0; // Normal/Deep
        }
    }

    /**
     * Gibt den Zone-Namen für Debug/Info zurück
     */
    public String getZoneName(int x, int z) {
        int zone = getZoneType(x, z);
        switch (zone) {
            case 1: return "Geode-Zone (Crimson Forest)";
            case 2: return "Kristall-Feld (Soul Sand Valley)";
            case 3: return "Tiefe Zone (Basalt Deltas)";
            default: return "Normal (Warped Forest)";
        }
    }

    /**
     * Gibt das entsprechende Biome für eine Zone zurück
     */
    public Biome getBiomeForZone(int zoneType) {
        switch (zoneType) {
            case 1: return BIOME_GEODE;
            case 2: return BIOME_CRYSTAL;
            case 3: return BIOME_DEEP;
            default: return BIOME_NORMAL;
        }
    }
}