package de.tecca.ultimatedimensions.dimension;

import de.tecca.ultimatedimensions.UltimateDimensions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DimensionRegistry {

    private static DimensionRegistry instance;
    private final UltimateDimensions plugin;
    private final Map<String, DimensionConfig> dimensions;

    private DimensionRegistry(UltimateDimensions plugin) {
        this.plugin = plugin;
        this.dimensions = new HashMap<>();
    }

    public static void initialize(UltimateDimensions plugin) {
        if (instance == null) {
            instance = new DimensionRegistry(plugin);
        }
    }

    public static DimensionRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DimensionRegistry not initialized!");
        }
        return instance;
    }

    public void registerDimension(String id, DimensionConfig config) {
        if (dimensions.containsKey(id)) {
            plugin.getLogger().warning("Dimension '" + id + "' already registered, overwriting...");
        }
        dimensions.put(id, config);
        plugin.getLogger().info("Registered dimension: " + id + " (" + config.getDisplayName() + ")");
    }

    public void unregisterDimension(String id) {
        if (dimensions.remove(id) != null) {
            plugin.getLogger().info("Unregistered dimension: " + id);
        }
    }

    public DimensionConfig getDimension(String id) {
        return dimensions.get(id);
    }

    public boolean hasDimension(String id) {
        return dimensions.containsKey(id);
    }

    public Collection<DimensionConfig> getAllDimensions() {
        return dimensions.values();
    }

    public Collection<String> getDimensionIds() {
        return dimensions.keySet();
    }

    public BaseDimensionGenerator createGenerator(String dimensionId, long seed) {
        DimensionConfig config = getDimension(dimensionId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown dimension: " + dimensionId);
        }

        Class<? extends BaseDimensionGenerator> generatorClass = config.getGeneratorClass();
        if (generatorClass == null) {
            throw new IllegalStateException("No generator class defined for dimension: " + dimensionId);
        }

        try {
            Constructor<? extends BaseDimensionGenerator> constructor =
                    generatorClass.getConstructor(UltimateDimensions.class, DimensionConfig.class, long.class);
            return constructor.newInstance(plugin, config, seed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create generator for dimension: " + dimensionId, e);
        }
    }

    public void loadFromConfig(FileConfiguration config) {
        if (!config.contains("dimensions")) {
            plugin.getLogger().warning("No dimensions configured in config.yml");
            return;
        }

        ConfigurationSection dimensionsSection = config.getConfigurationSection("dimensions");
        if (dimensionsSection == null) {
            return;
        }

        for (String dimensionId : dimensionsSection.getKeys(false)) {
            ConfigurationSection dimSection = dimensionsSection.getConfigurationSection(dimensionId);
            if (dimSection == null) {
                continue;
            }

            try {
                DimensionConfig dimConfig = DimensionConfig.fromConfig(dimensionId, dimSection);
                registerDimension(dimensionId, dimConfig);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load dimension '" + dimensionId + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void clear() {
        dimensions.clear();
    }
}