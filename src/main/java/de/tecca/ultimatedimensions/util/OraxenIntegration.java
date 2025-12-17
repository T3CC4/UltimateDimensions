package de.tecca.ultimatedimensions.util;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.*;

public class OraxenIntegration {

    private final Map<String, List<String>> oresByRarity = new HashMap<>();
    private boolean initialized = false;

    public OraxenIntegration() {
        initialize();
    }

    private void initialize() {
        try {
            String[] allItems = OraxenItems.getItemNames();

            if (allItems == null || allItems.length == 0) {
                return;
            }

            oresByRarity.put("common", new ArrayList<>());
            oresByRarity.put("rare", new ArrayList<>());
            oresByRarity.put("epic", new ArrayList<>());

            for (String itemId : allItems) {
                if (itemId == null) continue;

                if (itemId.toLowerCase().contains("ore")) {
                    String rarity = determineRarity(itemId);
                    oresByRarity.get(rarity).add(itemId);
                }
            }

            int totalOres = getOreCount();
            if (totalOres > 0) {
                initialized = true;
            }

        } catch (Exception e) {
            System.err.println("[UltimateDimensions] Fehler beim Initialisieren der Oraxen-Integration: " + e.getMessage());
        }
    }

    private String determineRarity(String itemId) {
        String lower = itemId.toLowerCase();

        if (lower.contains("epic") || lower.contains("legendary") ||
                lower.contains("mythic") || lower.contains("diamond") ||
                lower.contains("netherite")) {
            return "epic";
        }

        if (lower.contains("rare") || lower.contains("gold") ||
                lower.contains("emerald") || lower.contains("lapis")) {
            return "rare";
        }

        return "common";
    }

    public Material getRandomOre(Random random, String rarity) {
        if (!initialized) return null;

        List<String> ores = oresByRarity.get(rarity);
        if (ores == null || ores.isEmpty()) {
            return null;
        }

        try {
            String oreId = ores.get(random.nextInt(ores.size()));

            BlockData blockData = OraxenBlocks.getOraxenBlockData(oreId);
            if (blockData != null) {
                return blockData.getMaterial();
            }

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
}