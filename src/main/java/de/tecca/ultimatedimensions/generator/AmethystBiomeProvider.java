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
 * Generiert Terrain-Zonen die unabhängig vom Biome-System sind.
 * Biomes werden nur als "Base" für Minecraft-Effekte verwendet,
 * aber die eigentliche Terrain-Generation erfolgt über getZoneType().
 */
public class AmethystBiomeProvider extends BiomeProvider {

    private final SimplexOctaveGenerator biomeNoise;
    private final SimplexOctaveGenerator detailNoise;

    // Base-Biome (für Minecraft-interne Effekte, nicht für Terrain)
    // Wir verwenden ein neutrales Biome als Basis
    private static final Biome BASE_BIOME = Biome.THE_VOID;

    public AmethystBiomeProvider(long seed) {
        this.biomeNoise = new SimplexOctaveGenerator(new Random(seed), 4);
        this.biomeNoise.setScale(0.005); // Große Biom-Regionen

        this.detailNoise = new SimplexOctaveGenerator(new Random(seed + 1000), 3);
        this.detailNoise.setScale(0.02); // Details für Übergänge
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        // Alle Chunks bekommen das gleiche Base-Biome
        // Die eigentliche Variation erfolgt über getZoneType() im Generator
        return BASE_BIOME;
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        // Nur ein Biome für Minecraft
        return Arrays.asList(BASE_BIOME);
    }

    /**
     * Berechnet Zone-Type basierend auf Noise (unabhängig von Biome-Enum)
     * 0 = Normal, 1 = Geode, 2 = Crystal, 3 = Deep
     */
    public int getZoneType(int x, int z) {
        double biomeValue = biomeNoise.noise(x, z, 0.5, 0.5, true);
        double detailValue = detailNoise.noise(x, z, 0.5, 0.5, true);
        double combined = biomeValue + detailValue * 0.4;

        if (combined < -0.3) {
            return 2; // Crystal
        } else if (combined >= 0.1 && combined < 0.5) {
            return 1; // Geode
        } else if (detailValue > 0.6 && combined >= 0.5) {
            return 3; // Deep
        } else {
            return 0; // Normal
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
            case 1: return "Geode-Zone";
            case 2: return "Kristall-Feld";
            case 3: return "Tiefe Zone";
            default: return "Normal";
        }
    }
}