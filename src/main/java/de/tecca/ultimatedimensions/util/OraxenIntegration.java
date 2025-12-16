package de.tecca.ultimatedimensions.util;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Integration mit Oraxen für Custom Ore-Generierung
 *
 * Konfigurationsbeispiel für Oraxen-Items:
 *
 * In plugins/Oraxen/items/ores.yml:
 *
 * amethyst_copper_ore:
 *   material: PAPER
 *   Pack:
 *     generate_model: true
 *     parent_model: block/cube_all
 *     textures:
 *       - custom/ores/amethyst_copper_ore
 *   Mechanics:
 *     noteblock:
 *       custom_variation: 1
 *       model: custom_ore_1
 *   rarity: COMMON  # Custom-Tag für diese Integration
 */
public class OraxenIntegration {

    private final Map<String, List<String>> oresByRarity = new HashMap<>();
    private boolean initialized = false;

    public OraxenIntegration() {
        initialize();
    }

    private void initialize() {
        try {
            // Lade alle Oraxen-Items und sortiere sie nach Rarity
            String[] allItems = OraxenItems.getItemNames();

            oresByRarity.put("common", new ArrayList<>());
            oresByRarity.put("rare", new ArrayList<>());
            oresByRarity.put("epic", new ArrayList<>());

            for (String itemId : allItems) {
                // Nur Items die "ore" im Namen haben
                if (itemId.toLowerCase().contains("ore")) {
                    // Versuche Rarity aus dem Namen zu bestimmen
                    String rarity = determineRarity(itemId);
                    oresByRarity.get(rarity).add(itemId);
                }
            }

            initialized = true;

        } catch (Exception e) {
            System.err.println("Fehler beim Initialisieren der Oraxen-Integration: " + e.getMessage());
        }
    }

    private String determineRarity(String itemId) {
        String lower = itemId.toLowerCase();

        // Epic/Legendary Ores
        if (lower.contains("epic") || lower.contains("legendary") ||
                lower.contains("mythic") || lower.contains("diamond") ||
                lower.contains("netherite")) {
            return "epic";
        }

        // Rare Ores
        if (lower.contains("rare") || lower.contains("gold") ||
                lower.contains("emerald") || lower.contains("lapis")) {
            return "rare";
        }

        // Common Ores (default)
        return "common";
    }

    /**
     * Gibt ein zufälliges Oraxen-Ore basierend auf der Rarity zurück
     *
     * @param random Random-Generator
     * @param rarity "common", "rare", oder "epic"
     * @return Material oder null falls keine Ores verfügbar
     */
    public Material getRandomOre(Random random, String rarity) {
        if (!initialized) return null;

        List<String> ores = oresByRarity.get(rarity);
        if (ores == null || ores.isEmpty()) {
            return null;
        }

        try {
            String oreId = ores.get(random.nextInt(ores.size()));

            // Versuche das BlockData zu bekommen
            BlockData blockData = OraxenBlocks.getOraxenBlockData(oreId);
            if (blockData != null) {
                return blockData.getMaterial();
            }

            // Fallback: Versuche vom Item
            var item = OraxenItems.getItemById(oreId);
            if (item != null) {
                return item.build().getType();
            }

        } catch (Exception e) {
            // Fehler beim Abrufen des Ore-Blocks, ignorieren und Vanilla zurückgeben
        }

        return null;
    }

    /**
     * Prüft ob Oraxen-Ores verfügbar sind
     */
    public boolean hasOres() {
        return initialized && !oresByRarity.values().stream().allMatch(List::isEmpty);
    }

    /**
     * Gibt die Anzahl der verfügbaren Ores zurück
     */
    public int getOreCount() {
        return oresByRarity.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Debug-Methode: Listet alle gefundenen Ores
     */
    public void printAvailableOres() {
        System.out.println("=== Oraxen Ores ===");
        for (Map.Entry<String, List<String>> entry : oresByRarity.entrySet()) {
            System.out.println(entry.getKey().toUpperCase() + " (" + entry.getValue().size() + "):");
            for (String ore : entry.getValue()) {
                System.out.println("  - " + ore);
            }
        }
    }
}