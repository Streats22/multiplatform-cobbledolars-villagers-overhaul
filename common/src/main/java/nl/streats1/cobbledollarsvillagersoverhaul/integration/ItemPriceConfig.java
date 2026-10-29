package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemPriceConfig {
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";
    private static final String ITEM_PRICES_FILE = "item_prices.json";
    private static final String PRICES_KEY = "itemPrices";

    private ItemPriceConfig() {
    }

    private static Path getConfigFile() {
        return CobbleDollarsConfigHelper.getConfigDirectory()
                .resolve(CONFIG_SUBDIR)
                .resolve(ITEM_PRICES_FILE);
    }

    public static Map<String, Integer> loadEntries() {
        Path file = getConfigFile();
        if (!Files.isRegularFile(file)) return new LinkedHashMap<>();
        try {
            return parseEntries(Files.readString(file));
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    /** Raw file text for syncing to ops; empty object when the file does not exist yet. */
    public static String readRaw() {
        try {
            Path file = getConfigFile();
            return Files.isRegularFile(file) ? Files.readString(file) : "{}";
        } catch (Exception ex) {
            return "{}";
        }
    }

    public static void writeRaw(String json) throws java.io.IOException {
        Path file = getConfigFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    public static Map<String, Integer> parseEntries(String content) {
        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            Map<String, Integer> out = new LinkedHashMap<>();
            if (!root.has(PRICES_KEY) || !root.get(PRICES_KEY).isJsonObject()) return out;
            JsonObject prices = root.getAsJsonObject(PRICES_KEY);
            for (String key : prices.keySet()) {
                if (key == null || key.isEmpty()) continue;
                try {
                    int val = prices.get(key).getAsInt();
                    if (val > 0) out.put(key, val);
                } catch (Exception ignored) {
                }
            }
            return out;
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    public static void loadAndApply() {
        Map<String, Integer> entries = loadEntries();
        if (entries.isEmpty()) return;
        apply(entries);
    }

    /** Replaces the in-memory custom prices, including with an empty map when every price was removed. */
    public static void apply(Map<String, Integer> entries) {
        DatapackItemPricing.loadCustomPrices(new Gson().toJson(entries));
    }

    public static void saveEntries(Map<String, Integer> entries) {
        try {
            writeRaw(toJson(entries));
        } catch (Exception ex) {
        }
    }

    public static String toJson(Map<String, Integer> entries) {
        JsonObject prices = new JsonObject();
        for (Map.Entry<String, Integer> e : entries.entrySet()) {
            if (e.getKey() != null && !e.getKey().isEmpty() && e.getValue() != null && e.getValue() > 0) {
                prices.addProperty(e.getKey(), e.getValue());
            }
        }
        JsonObject root = new JsonObject();
        root.add(PRICES_KEY, prices);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
}
