package de.tecca.ultimatedimensions.commands;

import de.tecca.ultimatedimensions.UltimateDimensions;
import de.tecca.ultimatedimensions.generator.AmethystDimensionGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import de.tecca.ultimatedimensions.generator.AmethystBiomeProvider;
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
            case "biome":
            case "findbiome":
                return handleFindBiome(sender, args);
            case "info":
                return handleInfo(sender);
            case "testbiomes":
                return handleTestBiomes(sender);
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

        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /dimension create <name> [seed]");
            return true;
        }

        String worldName = args[1];

        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage("§cWelt '" + worldName + "' existiert bereits!");
            return true;
        }

        long seed = args.length >= 3 ? parseSeed(args[2]) : System.currentTimeMillis();

        sender.sendMessage("§aErstelle Amethyst-Dimension '" + worldName + "'...");

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                AmethystDimensionGenerator generator = plugin.getOrCreateGenerator(worldName);

                WorldCreator creator = new WorldCreator(worldName)
                        .environment(World.Environment.NETHER)
                        .generator(generator)
                        .generateStructures(false)
                        .seed(seed);

                World world = creator.createWorld();

                if (world != null) {
                    world.setSpawnLocation(0, 64, 0);

                    // Welt in Config speichern
                    plugin.saveWorld(worldName, seed, World.Environment.NETHER);

                    sender.sendMessage("§aWelt '" + worldName + "' erfolgreich erstellt!");
                    sender.sendMessage("§7Seed: §e" + seed);
                    if (plugin.isOraxenAvailable()) {
                        sender.sendMessage("§7Oraxen Custom Ores: §aAktiviert");
                    }
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

        // Mit Koordinaten?
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
            // Spawn-Location
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

        // Spieler aus der Welt teleportieren
        world.getPlayers().forEach(p -> {
            World defaultWorld = Bukkit.getWorlds().get(0);
            p.teleport(defaultWorld.getSpawnLocation());
            p.sendMessage("§cDie Welt '" + worldName + "' wird gelöscht. Du wurdest zur Hauptwelt teleportiert.");
        });

        boolean unloaded = Bukkit.unloadWorld(world, false);

        if (unloaded) {
            // Aus Config entfernen
            plugin.removeWorld(worldName);

            sender.sendMessage("§aWelt '" + worldName + "' wurde entladen!");
            sender.sendMessage("§7Die Weltdateien müssen manuell gelöscht werden.");
        } else {
            sender.sendMessage("§cFehler beim Entladen der Welt!");
        }

        return true;
    }

    private boolean handleFindBiome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        if (!sender.hasPermission("ultimatedimensions.findbiome")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (!(world.getGenerator() instanceof AmethystDimensionGenerator)) {
            sender.sendMessage("§cDu musst in einer Amethyst-Dimension sein!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /dimension biome <zone>");
            sender.sendMessage("§7Verfügbare Zonen:");
            sender.sendMessage("§e  - normal §7(Standard Amethyst-Dichte)");
            sender.sendMessage("§e  - geode §7(Hohe Dichte, schwebende Inseln)");
            sender.sendMessage("§e  - crystal §7(Sehr viele Cluster)");
            sender.sendMessage("§e  - deep §7(Mehr Gestein, dunkler)");
            return true;
        }

        String zoneType = args[1].toLowerCase();
        int targetZone = -1;

        if (zoneType.equals("normal")) {
            targetZone = 0;
        } else if (zoneType.equals("geode")) {
            targetZone = 1;
        } else if (zoneType.equals("crystal")) {
            targetZone = 2;
        } else if (zoneType.equals("deep")) {
            targetZone = 3;
        }

        if (targetZone == -1) {
            sender.sendMessage("§cUnbekannte Zone: " + zoneType);
            return true;
        }

        sender.sendMessage("§7Suche nach §e" + getZoneDisplayName(targetZone) + "§7...");

        Location start = player.getLocation();
        int searchRadius = 5000;
        int stepSize = 16;

        int finalTargetZone = targetZone;
        AmethystDimensionGenerator gen = (AmethystDimensionGenerator) world.getGenerator();
        AmethystBiomeProvider provider = gen.getBiomeProvider();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location found = null;
            double closestDistance = Double.MAX_VALUE;

            for (int x = -searchRadius; x < searchRadius; x += stepSize) {
                for (int z = -searchRadius; z < searchRadius; z += stepSize) {
                    Location checkLoc = new Location(world, start.getX() + x, 64, start.getZ() + z);
                    int zone = provider.getZoneType(checkLoc.getBlockX(), checkLoc.getBlockZ());

                    if (zone == finalTargetZone) {
                        double distance = start.distance(checkLoc);
                        if (distance < closestDistance && distance > 50) {
                            closestDistance = distance;
                            found = checkLoc;
                        }
                    }
                }
            }

            Location finalFound = found;
            double finalDistance = closestDistance;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalFound != null) {
                    int x = finalFound.getBlockX();
                    int z = finalFound.getBlockZ();

                    sender.sendMessage("§a✓ Zone gefunden!");
                    sender.sendMessage("§7Koordinaten: §e" + x + " / " + z);
                    sender.sendMessage("§7Distanz: §e" + String.format("%.0f", finalDistance) + " Blöcke");
                    sender.sendMessage("§7Teleport: §e/dimension tp " + world.getName() + " " + x + " 64 " + z);
                } else {
                    sender.sendMessage("§cKeine " + getZoneDisplayName(finalTargetZone) + " in " + searchRadius + " Blöcken Umkreis gefunden!");
                }
            });
        });

        return true;
    }

    private String getZoneDisplayName(int zone) {
        switch (zone) {
            case 0: return "Normal-Zone";
            case 1: return "Geode-Zone";
            case 2: return "Kristall-Feld";
            case 3: return "Tiefe Zone";
            default: return "Unbekannt";
        }
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        if (!sender.hasPermission("ultimatedimensions.info")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (!(world.getGenerator() instanceof AmethystDimensionGenerator)) {
            sender.sendMessage("§cDu musst in einer Amethyst-Dimension sein!");
            return true;
        }

        Location loc = player.getLocation();

        AmethystDimensionGenerator gen = (AmethystDimensionGenerator) world.getGenerator();
        AmethystBiomeProvider provider = gen.getBiomeProvider();

        String zoneName = "Unbekannt";
        String zoneDesc = "";

        if (provider != null) {
            int zoneType = provider.getZoneType(loc.getBlockX(), loc.getBlockZ());
            zoneName = provider.getZoneName(loc.getBlockX(), loc.getBlockZ());
            zoneDesc = getZoneDescription(zoneType);
        }

        sender.sendMessage("§6=== Amethyst-Dimensions-Info ===");
        sender.sendMessage("§7Welt: §e" + world.getName());
        sender.sendMessage("§7Seed: §e" + world.getSeed());
        sender.sendMessage("§7Position: §e" + loc.getBlockX() + " / " + loc.getBlockY() + " / " + loc.getBlockZ());
        sender.sendMessage("§7Terrain-Zone: §e" + zoneName);
        if (!zoneDesc.isEmpty()) {
            sender.sendMessage("§7" + zoneDesc);
        }

        return true;
    }

    private String getBiomeDisplayName(Biome biome) {
        // Nicht mehr benötigt - alle Chunks haben THE_VOID
        return "Amethyst Dimension";
    }

    private String getZoneDescription(int zoneType) {
        switch (zoneType) {
            case 0:
                return "Normale Amethyst-Dichte, ausgeglichenes Terrain";
            case 1:
                return "Geode-Zone: Sehr hohe Amethyst-Konzentration, schwebende Inseln";
            case 2:
                return "Kristall-Feld: Extrem viele Cluster und Budding Amethyst";
            case 3:
                return "Tiefe Zone: Mehr Gestein, dunklere Atmosphäre";
            default:
                return "";
        }
    }

    private boolean handleTestBiomes(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (!(world.getGenerator() instanceof AmethystDimensionGenerator)) {
            sender.sendMessage("§cDu musst in einer Amethyst-Dimension sein!");
            return true;
        }

        AmethystDimensionGenerator gen = (AmethystDimensionGenerator) world.getGenerator();
        AmethystBiomeProvider provider = gen.getBiomeProvider();

        if (provider == null) {
            sender.sendMessage("§cBiomeProvider ist null!");
            return true;
        }

        sender.sendMessage("§6=== Zonen-Test ===");
        Location loc = player.getLocation();
        int startX = loc.getBlockX() - 50;
        int startZ = loc.getBlockZ() - 50;

        int[] zoneCounts = new int[4];

        for (int x = 0; x < 100; x += 10) {
            for (int z = 0; z < 100; z += 10) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                int zoneType = provider.getZoneType(worldX, worldZ);
                zoneCounts[zoneType]++;
            }
        }

        sender.sendMessage("§7Zonen-Verteilung (100 Samples):");
        sender.sendMessage("§e  Normal: §f" + zoneCounts[0] + " §7(" + (zoneCounts[0]) + "%)");
        sender.sendMessage("§e  Geode: §f" + zoneCounts[1] + " §7(" + (zoneCounts[1]) + "%)");
        sender.sendMessage("§e  Crystal: §f" + zoneCounts[2] + " §7(" + (zoneCounts[2]) + "%)");
        sender.sendMessage("§e  Deep: §f" + zoneCounts[3] + " §7(" + (zoneCounts[3]) + "%)");

        int currentZone = provider.getZoneType(loc.getBlockX(), loc.getBlockZ());
        sender.sendMessage("§7Deine Zone: §e" + provider.getZoneName(loc.getBlockX(), loc.getBlockZ()));

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("ultimatedimensions.list")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        sender.sendMessage("§6=== Amethyst-Dimensionen ===");
        boolean found = false;

        for (World world : Bukkit.getWorlds()) {
            if (world.getGenerator() instanceof AmethystDimensionGenerator) {
                sender.sendMessage("§e- §a" + world.getName() + " §7(Seed: " + world.getSeed() + ")");
                found = true;
            }
        }

        if (!found) {
            sender.sendMessage("§7Keine Amethyst-Dimensionen gefunden.");
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
        sender.sendMessage("§e/dimension create <name> [seed] §7- Erstelle eine Amethyst-Dimension");
        sender.sendMessage("§e/dimension tp <welt> [x y z] §7- Teleportiere zu einer Dimension");
        sender.sendMessage("§e/dimension delete <welt> §7- Entlade eine Dimension");
        sender.sendMessage("§e/dimension list §7- Liste alle Amethyst-Dimensionen");
        sender.sendMessage("§e/dimension biome <typ> §7- Finde Zone (normal/geode/crystal/deep)");
        sender.sendMessage("§e/dimension info §7- Zeige Info über aktuelle Zone");
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            return filterStartingWith(Arrays.asList("create", "tp", "teleport", "delete", "list", "biome", "findbiome", "info"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport") ||
                    args[0].equalsIgnoreCase("delete")) {
                List<String> worlds = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    if (world.getGenerator() instanceof AmethystDimensionGenerator) {
                        worlds.add(world.getName());
                    }
                }
                return filterStartingWith(worlds, args[1]);
            }

            if (args[0].equalsIgnoreCase("biome") || args[0].equalsIgnoreCase("findbiome")) {
                return filterStartingWith(Arrays.asList("normal", "geode", "crystal", "deep"), args[1]);
            }
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