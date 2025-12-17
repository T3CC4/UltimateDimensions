package de.tecca.ultimatedimensions.util;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Integration mit Oraxen für Custom Ore-Generierung
 */
public class OraxenIntegration {

    private final Map<String, List<String>> oresByRarity = new HashMap<>();
    private boolean initialized = false;

    public OraxenIntegration() {
        initialize();
    }

    private void initialize() {
        try {
            // Null-Check für Oraxen Items
            String[] allItems = OraxenItems.getItemNames();

            if (allItems == null || allItems.length == 0) {
                System.out.println("[UltimateDimensions] Oraxen: Keine Items gefunden (Oraxen noch nicht vollständig geladen)");
                return;
            }

            oresByRarity.put("common", new ArrayList<>());
            oresByRarity.put("rare", new ArrayList<>());
            oresByRarity.put("epic", new ArrayList<>());

            for (String itemId : allItems) {
                if (itemId == null) continue;

                // Nur Items die "ore" im Namen haben
                if (itemId.toLowerCase().contains("ore")) {
                    String rarity = determineRarity(itemId);
                    oresByRarity.get(rarity).add(itemId);
                }
            }

            int totalOres = getOreCount();
            if (totalOres > 0) {
                initialized = true;
                System.out.println("[UltimateDimensions] Oraxen: " + totalOres + " Custom Ores geladen");
            } else {
                System.out.println("[UltimateDimensions] Oraxen: Keine Custom Ores gefunden");
            }

        } catch (Exception e) {
            System.err.println("[UltimateDimensions] Fehler beim Initialisieren der Oraxen-Integration: " + e.getMessage());
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
            // Fehler beim Abrufen des Ore-Blocks, ignorieren
        }

        return null;
    }

    public boolean hasOres() {
        return initialized && !oresByRarity.values().stream().allMatch(List::isEmpty);
    }

    public int getOreCount() {
        return oresByRarity.values().stream().mapToInt(List::size).sum();
    }

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