package de.tecca.ultimatedimensions.commands;

import de.tecca.ultimatedimensions.UltimateDimensions;
import de.tecca.ultimatedimensions.dimension.BaseDimensionGenerator;
import de.tecca.ultimatedimensions.dimension.DimensionConfig;
import de.tecca.ultimatedimensions.dimension.DimensionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DimensionCommand implements CommandExecutor, TabCompleter {

    private final UltimateDimensions plugin;

    public DimensionCommand(UltimateDimensions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(sender, args);
            case "tp":
            case "teleport":
                return handleTeleport(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "list":
                return handleList(sender);
            case "types":
                return handleTypes(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatedimensions.create")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cNutzung: /dimension create <weltname> <typ> [seed]");
            sender.sendMessage("§7Verfügbare Typen: /dimension types");
            return true;
        }

        String worldName = args[1];
        String dimensionType = args[2];

        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage("§cWelt '" + worldName + "' existiert bereits!");
            return true;
        }

        DimensionRegistry registry = DimensionRegistry.getInstance();
        if (!registry.hasDimension(dimensionType)) {
            sender.sendMessage("§cUnbekannter Dimensions-Typ: " + dimensionType);
            sender.sendMessage("§7Verfügbare Typen: /dimension types");
            return true;
        }

        long seed = args.length >= 4 ? parseSeed(args[3]) : System.currentTimeMillis();

        sender.sendMessage("§aErstelle Dimension '" + worldName + "' (Typ: " + dimensionType + ")...");

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                DimensionConfig config = registry.getDimension(dimensionType);
                BaseDimensionGenerator generator = registry.createGenerator(dimensionType, seed);

                WorldCreator creator = new WorldCreator(worldName)
                        .environment(config.getEnvironment())
                        .generator(generator)
                        .generateStructures(config.hasFeature("structures"))
                        .seed(seed);

                World world = creator.createWorld();

                if (world != null) {
                    world.setSpawnLocation(0, 64, 0);
                    plugin.saveWorld(worldName, seed, config.getEnvironment(), dimensionType);

                    sender.sendMessage("§aWelt '" + worldName + "' erfolgreich erstellt!");
                    sender.sendMessage("§7Typ: §e" + config.getDisplayName());
                    sender.sendMessage("§7Seed: §e" + seed);
                } else {
                    sender.sendMessage("§cFehler beim Erstellen der Welt!");
                }
            } catch (Exception e) {
                sender.sendMessage("§cFehler: " + e.getMessage());
                plugin.getLogger().severe("Fehler beim Erstellen der Welt: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        if (!sender.hasPermission("ultimatedimensions.teleport")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /dimension tp <welt> [x] [y] [z]");
            return true;
        }

        Player player = (Player) sender;
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage("§cWelt '" + worldName + "' existiert nicht!");
            return true;
        }

        Location targetLocation;

        if (args.length >= 5) {
            try {
                int x = Integer.parseInt(args[2]);
                int y = Integer.parseInt(args[3]);
                int z = Integer.parseInt(args[4]);
                targetLocation = new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUngültige Koordinaten!");
                return true;
            }
        } else {
            targetLocation = world.getSpawnLocation();
        }

        player.teleport(targetLocation);
        sender.sendMessage("§aTeleportiert nach '" + worldName + "'!");
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatedimensions.delete")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /dimension delete <welt>");
            return true;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage("§cWelt '" + worldName + "' ist nicht geladen!");
            return true;
        }

        world.getPlayers().forEach(p -> {
            World defaultWorld = Bukkit.getWorlds().get(0);
            p.teleport(defaultWorld.getSpawnLocation());
            p.sendMessage("§cDie Welt '" + worldName + "' wird gelöscht.");
        });

        boolean unloaded = Bukkit.unloadWorld(world, false);

        if (unloaded) {
            plugin.removeWorld(worldName);
            sender.sendMessage("§aWelt '" + worldName + "' wurde entladen!");
        } else {
            sender.sendMessage("§cFehler beim Entladen der Welt!");
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("ultimatedimensions.list")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        sender.sendMessage("§6=== Custom Dimensionen ===");
        boolean found = false;

        for (World world : Bukkit.getWorlds()) {
            if (world.getGenerator() instanceof BaseDimensionGenerator) {
                BaseDimensionGenerator gen = (BaseDimensionGenerator) world.getGenerator();
                sender.sendMessage("§e- §a" + world.getName() +
                        " §7(Typ: " + gen.getConfig().getId() + ", Seed: " + world.getSeed() + ")");
                found = true;
            }
        }

        if (!found) {
            sender.sendMessage("§7Keine Custom Dimensionen gefunden.");
        }

        return true;
    }

    private boolean handleTypes(CommandSender sender) {
        if (!sender.hasPermission("ultimatedimensions.list")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        DimensionRegistry registry = DimensionRegistry.getInstance();
        sender.sendMessage("§6=== Verfügbare Dimensions-Typen ===");

        for (DimensionConfig config : registry.getAllDimensions()) {
            sender.sendMessage("§e" + config.getId() + " §7- " + config.getDisplayName());
            sender.sendMessage("  §7Environment: §f" + config.getEnvironment().name());
            if (!config.getFeatures().isEmpty()) {
                sender.sendMessage("  §7Features: §f" + String.join(", ", config.getFeatures()));
            }
        }

        return true;
    }

    private long parseSeed(String seedStr) {
        try {
            return Long.parseLong(seedStr);
        } catch (NumberFormatException e) {
            return seedStr.hashCode();
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== UltimateDimensions Commands ===");
        sender.sendMessage("§e/dimension create <name> <typ> [seed] §7- Erstelle Dimension");
        sender.sendMessage("§e/dimension tp <welt> [x y z] §7- Teleportiere zu Dimension");
        sender.sendMessage("§e/dimension delete <welt> §7- Entlade Dimension");
        sender.sendMessage("§e/dimension list §7- Liste alle Custom Dimensionen");
        sender.sendMessage("§e/dimension types §7- Zeige verfügbare Dimensions-Typen");
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            return filterStartingWith(Arrays.asList("create", "tp", "teleport", "delete", "list", "types"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport") ||
                    args[0].equalsIgnoreCase("delete")) {
                List<String> worlds = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    if (world.getGenerator() instanceof BaseDimensionGenerator) {
                        worlds.add(world.getName());
                    }
                }
                return filterStartingWith(worlds, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return filterStartingWith(
                    new ArrayList<>(DimensionRegistry.getInstance().getDimensionIds()),
                    args[2]
            );
        }

        return new ArrayList<>();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lowerPrefix)) {
                result.add(s);
            }
        }
        return result;
    }
}