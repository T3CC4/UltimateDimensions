package de.tecca.ultimatedimensions.dimension;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimensionConfig {

    private String id;
    private String displayName;
    private Class<? extends BaseDimensionGenerator> generatorClass;
    private World.Environment environment;
    private List<String> features;
    private Map<String, Object> customSettings;

    public DimensionConfig(String id) {
        this.id = id;
        this.displayName = id;
        this.environment = World.Environment.NORMAL;
        this.features = new ArrayList<>();
        this.customSettings = new HashMap<>();
    }

    public static DimensionConfig fromConfig(String id, ConfigurationSection section) {
        DimensionConfig config = new DimensionConfig(id);

        config.displayName = section.getString("display_name", id);

        String envName = section.getString("environment", "NORMAL");
        try {
            config.environment = World.Environment.valueOf(envName.toUpperCase());
        } catch (IllegalArgumentException e) {
            config.environment = World.Environment.NORMAL;
        }

        if (section.contains("features")) {
            config.features = section.getStringList("features");
        }

        if (section.contains("settings")) {
            ConfigurationSection settings = section.getConfigurationSection("settings");
            if (settings != null) {
                for (String key : settings.getKeys(false)) {
                    config.customSettings.put(key, settings.get(key));
                }
            }
        }

        String generatorClassName = section.getString("generator_class");
        if (generatorClassName != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends BaseDimensionGenerator> clazz =
                        (Class<? extends BaseDimensionGenerator>) Class.forName(generatorClassName);
                config.generatorClass = clazz;
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new RuntimeException("Invalid generator class: " + generatorClassName, e);
            }
        }

        return config;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Class<? extends BaseDimensionGenerator> getGeneratorClass() { return generatorClass; }
    public World.Environment getEnvironment() { return environment; }
    public List<String> getFeatures() { return features; }
    public Map<String, Object> getCustomSettings() { return customSettings; }

    public DimensionConfig setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public DimensionConfig setGeneratorClass(Class<? extends BaseDimensionGenerator> generatorClass) {
        this.generatorClass = generatorClass;
        return this;
    }

    public DimensionConfig setEnvironment(World.Environment environment) {
        this.environment = environment;
        return this;
    }

    public DimensionConfig addFeature(String feature) {
        this.features.add(feature);
        return this;
    }

    public DimensionConfig setSetting(String key, Object value) {
        this.customSettings.put(key, value);
        return this;
    }

    public boolean hasFeature(String feature) {
        return features.contains(feature);
    }

    public Object getSetting(String key, Object defaultValue) {
        return customSettings.getOrDefault(key, defaultValue);
    }
}