package de.tecca.ultimatedimensions;

import de.tecca.ultimatedimensions.commands.DimensionCommand;
import de.tecca.ultimatedimensions.generator.AmethystDimensionGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UltimateDimensions extends JavaPlugin {

    private static UltimateDimensions instance;
    private final Map<String, AmethystDimensionGenerator> generators = new HashMap<>();
    private boolean oraxenAvailable = false;
    private FileConfiguration worldsConfig;
    private File worldsConfigFile;

    @Override
    public void onEnable() {
        instance = this;

        // Config erstellen
        saveDefaultConfig();
        loadWorldsConfig();

        // Oraxen-Kompatibilität prüfen
        if (Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
            oraxenAvailable = true;
            getLogger().info("Oraxen gefunden - Custom Ores aktiviert!");
        } else {
            getLogger().warning("Oraxen nicht gefunden - Custom Ores deaktiviert");
        }

        // Commands registrieren
        getCommand("dimension").setExecutor(new DimensionCommand(this));

        // Gespeicherte Welten laden
        loadSavedWorlds();

        getLogger().info("UltimateDimensions erfolgreich geladen!");
    }

    @Override
    public void onDisable() {
        // Welten speichern
        saveAllWorlds();
        generators.clear();
    }

    private void loadWorldsConfig() {
        worldsConfigFile = new File(getDataFolder(), "worlds.yml");
        if (!worldsConfigFile.exists()) {
            worldsConfigFile.getParentFile().mkdirs();
            saveResource("worlds.yml", false);
        }
        worldsConfig = YamlConfiguration.loadConfiguration(worldsConfigFile);
    }

    private void saveWorldsConfig() {
        try {
            worldsConfig.save(worldsConfigFile);
        } catch (Exception e) {
            getLogger().severe("Fehler beim Speichern der worlds.yml: " + e.getMessage());
        }
    }

    private void loadSavedWorlds() {
        if (!worldsConfig.contains("worlds")) {
            return;
        }

        ConfigurationSection section = worldsConfig.getConfigurationSection("worlds");
        if (section == null) return;

        for (String worldName : section.getKeys(false)) {
            long seed = section.getLong(worldName + ".seed", 0);
            String envStr = section.getString(worldName + ".environment", "NETHER");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                getLogger().info("Lade gespeicherte Welt: " + worldName);

                World.Environment environment;
                try {
                    environment = World.Environment.valueOf(envStr);
                } catch (IllegalArgumentException e) {
                    environment = World.Environment.NETHER;
                }

                WorldCreator creator = new WorldCreator(worldName)
                        .environment(environment)
                        .generator(getOrCreateGenerator(worldName))
                        .generateStructures(true)
                        .seed(seed);

                world = creator.createWorld();
                if (world != null) {
                    world.setSpawnLocation(0, 64, 0);
                }
            }
        }
    }

    public void saveWorld(String worldName, long seed, World.Environment environment) {
        worldsConfig.set("worlds." + worldName + ".seed", seed);
        worldsConfig.set("worlds." + worldName + ".environment", environment.name());
        saveWorldsConfig();
        getLogger().info("Welt gespeichert: " + worldName);
    }

    public void removeWorld(String worldName) {
        worldsConfig.set("worlds." + worldName, null);
        saveWorldsConfig();
        getLogger().info("Welt entfernt: " + worldName);
    }

    private void saveAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getGenerator() instanceof AmethystDimensionGenerator) {
                saveWorld(world.getName(), world.getSeed(), world.getEnvironment());
            }
        }
    }

    public AmethystDimensionGenerator getOrCreateGenerator(String worldName) {
        return generators.computeIfAbsent(worldName, k -> new AmethystDimensionGenerator(this));
    }

    public boolean isOraxenAvailable() {
        return oraxenAvailable;
    }

    @Override
    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return getOrCreateGenerator(worldName);
    }

    public static UltimateDimensions getInstance() {
        return instance;
    }
}