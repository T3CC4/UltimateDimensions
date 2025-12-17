package de.tecca.ultimatedimensions;

import de.tecca.ultimatedimensions.commands.DimensionCommand;
import de.tecca.ultimatedimensions.dimension.BaseDimensionGenerator;
import de.tecca.ultimatedimensions.dimension.DimensionConfig;
import de.tecca.ultimatedimensions.dimension.DimensionRegistry;
import de.tecca.ultimatedimensions.dimensions.amethyst.AmethystDimensionGenerator;
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

public class UltimateDimensions extends JavaPlugin {

    private static UltimateDimensions instance;
    private boolean oraxenAvailable = false;
    private FileConfiguration worldsConfig;
    private File worldsConfigFile;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadWorldsConfig();

        if (Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
            oraxenAvailable = true;
            getLogger().info("Oraxen gefunden - Custom Ores aktiviert!");
        }

        DimensionRegistry.initialize(this);
        registerBuiltInDimensions();
        DimensionRegistry.getInstance().loadFromConfig(getConfig());

        getCommand("dimension").setExecutor(new DimensionCommand(this));

        Bukkit.getScheduler().runTask(this, this::loadSavedWorlds);

        getLogger().info("UltimateDimensions erfolgreich geladen!");
        getLogger().info("Registrierte Dimensions-Typen: " +
                DimensionRegistry.getInstance().getDimensionIds().size());
    }

    @Override
    public void onDisable() {
        saveAllWorlds();
    }

    private void registerBuiltInDimensions() {
        DimensionConfig amethystConfig = new DimensionConfig("amethyst")
                .setDisplayName("Amethyst Dimension")
                .setGeneratorClass(AmethystDimensionGenerator.class)
                .setEnvironment(World.Environment.NETHER)
                .addFeature("custom_ores")
                .addFeature("floating_islands")
                .addFeature("crystal_caves");

        DimensionRegistry.getInstance().registerDimension("amethyst", amethystConfig);
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
            String envStr = section.getString(worldName + ".environment", "NORMAL");
            String dimensionType = section.getString(worldName + ".dimension_type", "amethyst");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                getLogger().info("Lade gespeicherte Welt: " + worldName + " (Typ: " + dimensionType + ")");

                World.Environment environment;
                try {
                    environment = World.Environment.valueOf(envStr);
                } catch (IllegalArgumentException e) {
                    environment = World.Environment.NORMAL;
                }

                try {
                    DimensionRegistry registry = DimensionRegistry.getInstance();
                    if (!registry.hasDimension(dimensionType)) {
                        getLogger().warning("Unbekannter Dimensions-Typ für Welt " + worldName + ": " + dimensionType);
                        continue;
                    }

                    BaseDimensionGenerator generator = registry.createGenerator(dimensionType, seed);

                    WorldCreator creator = new WorldCreator(worldName)
                            .environment(environment)
                            .generator(generator)
                            .generateStructures(true)
                            .seed(seed);

                    world = creator.createWorld();
                    if (world != null) {
                        world.setSpawnLocation(0, 64, 0);
                        getLogger().info("Welt geladen: " + worldName);
                    } else {
                        getLogger().warning("Konnte Welt nicht laden: " + worldName);
                    }
                } catch (Exception e) {
                    getLogger().severe("Fehler beim Laden der Welt " + worldName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveWorld(String worldName, long seed, World.Environment environment, String dimensionType) {
        worldsConfig.set("worlds." + worldName + ".seed", seed);
        worldsConfig.set("worlds." + worldName + ".environment", environment.name());
        worldsConfig.set("worlds." + worldName + ".dimension_type", dimensionType);
        saveWorldsConfig();
        getLogger().info("Welt gespeichert: " + worldName + " (Typ: " + dimensionType + ")");
    }

    public void removeWorld(String worldName) {
        worldsConfig.set("worlds." + worldName, null);
        saveWorldsConfig();
        getLogger().info("Welt entfernt: " + worldName);
    }

    private void saveAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getGenerator() instanceof BaseDimensionGenerator) {
                BaseDimensionGenerator gen = (BaseDimensionGenerator) world.getGenerator();
                saveWorld(world.getName(), world.getSeed(), world.getEnvironment(),
                        gen.getConfig().getId());
            }
        }
    }

    public boolean isOraxenAvailable() {
        return oraxenAvailable;
    }

    @Override
    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (id == null || id.isEmpty()) {
            id = "amethyst";
        }

        try {
            DimensionRegistry registry = DimensionRegistry.getInstance();
            if (registry.hasDimension(id)) {
                long seed = System.currentTimeMillis();
                return registry.createGenerator(id, seed);
            }
        } catch (Exception e) {
            getLogger().severe("Failed to create generator for world " + worldName + ": " + e.getMessage());
        }

        return null;
    }

    public DimensionRegistry getDimensionRegistry() {
        return DimensionRegistry.getInstance();
    }

    public static UltimateDimensions getInstance() {
        return instance;
    }
}