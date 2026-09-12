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
            String content = Files.readString(file);
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
        DatapackItemPricing.loadCustomPrices(new Gson().toJson(entries));
    }

    public static void saveEntries(Map<String, Integer> entries) {
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            JsonObject prices = new JsonObject();
            for (Map.Entry<String, Integer> e : entries.entrySet()) {
                if (e.getKey() != null && !e.getKey().isEmpty() && e.getValue() != null && e.getValue() > 0) {
                    prices.addProperty(e.getKey(), e.getValue());
                }
            }
            JsonObject root = new JsonObject();
            root.add(PRICES_KEY, prices);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ex) {
        }
    }
}
