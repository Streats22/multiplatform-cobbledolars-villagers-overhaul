package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read/write CobbleDollars default_shop.json for the shop editor.
 * Supports multiple categories (CobbleDollars format).
 * Format: { "defaultShop": [ { "Category Name": [ { "item": "id", "price": X }, ... ] }, ... ] }
 */
public final class DefaultShopConfig {
    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";
    private static final String DEFAULT_SHOP_FILE = "default_shop.json";
    private static final String DEFAULT_SHOP_KEY = "defaultShop";
    private static final String DEFAULT_CATEGORY = "Default";

    private DefaultShopConfig() {
    }

    private static Path getShopFile() {
        return CobbleDollarsConfigHelper.getConfigDirectory().resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(DEFAULT_SHOP_FILE);
    }

    /**
     * Load all categories with their offers. Preserves order.
     */
    public static Map<String, List<ShopEntryRecord>> loadCategories() {
        Path file = getShopFile();
        Map<String, List<ShopEntryRecord>> out = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            out.put(DEFAULT_CATEGORY, new ArrayList<>());
            return out;
        }
        try {
            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has(DEFAULT_SHOP_KEY)) {
                out.put(DEFAULT_CATEGORY, new ArrayList<>());
                return out;
            }
            JsonElement arrEl = root.get(DEFAULT_SHOP_KEY);
            if (!arrEl.isJsonArray()) {
                out.put(DEFAULT_CATEGORY, new ArrayList<>());
                return out;
            }
            for (JsonElement catEl : arrEl.getAsJsonArray()) {
                if (!catEl.isJsonObject()) continue;
                JsonObject catObj = catEl.getAsJsonObject();
                for (String catName : catObj.keySet()) {
                    if (catName == null || catName.isBlank()) continue;
                    List<ShopEntryRecord> offers = new ArrayList<>();
                    JsonElement offersEl = catObj.get(catName);
                    if (offersEl != null && offersEl.isJsonArray()) {
                        for (JsonElement e : offersEl.getAsJsonArray()) {
                            if (!e.isJsonObject()) continue;
                            JsonObject o = e.getAsJsonObject();
                            String itemId = o.has("item") ? o.get("item").getAsString() : null;
                            if (itemId == null || itemId.isEmpty()) continue;
                            int price = o.has("price") ? parsePrice(o.get("price")) : 0;
                            if (price <= 0) continue;
                            offers.add(new ShopEntryRecord(itemId, price));
                        }
                    }
                    out.put(catName, offers);
                }
            }
            if (out.isEmpty()) out.put(DEFAULT_CATEGORY, new ArrayList<>());
        } catch (Exception ex) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to load default shop config: {}", ex.getMessage());
            out.put(DEFAULT_CATEGORY, new ArrayList<>());
        }
        return out;
    }

    /**
     * @deprecated Use loadCategories()
     */
    public static List<ShopEntryRecord> loadEntries() {
        Map<String, List<ShopEntryRecord>> cats = loadCategories();
        List<ShopEntryRecord> out = new ArrayList<>();
        for (List<ShopEntryRecord> list : cats.values()) out.addAll(list);
        return out;
    }

    private static int parsePrice(JsonElement el) {
        if (el == null || el.isJsonNull()) return 0;
        if (el.isJsonPrimitive()) {
            var p = el.getAsJsonPrimitive();
            if (p.isNumber()) return p.getAsInt();
            if (p.isString()) {
                String s = p.getAsString().trim().toLowerCase();
                int mult = 1;
                if (s.endsWith("k")) {
                    mult = 1000;
                    s = s.substring(0, s.length() - 1);
                } else if (s.endsWith("m")) {
                    mult = 1_000_000;
                    s = s.substring(0, s.length() - 1);
                }
                try {
                    return (int) (Double.parseDouble(s) * mult);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    /**
     * Save categories with their offers. CobbleDollars format.
     */
    public static void saveCategories(Map<String, List<ShopEntryRecord>> categories) {
        Path file = getShopFile();
        try {
            Files.createDirectories(file.getParent());
            JsonArray defaultShop = new JsonArray();
            for (Map.Entry<String, List<ShopEntryRecord>> entry : categories.entrySet()) {
                String catName = entry.getKey();
                if (catName == null || catName.isBlank()) continue;
                JsonArray offers = new JsonArray();
                for (ShopEntryRecord e : entry.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("item", e.itemId());
                    o.addProperty("price", e.price());
                    offers.add(o);
                }
                JsonObject category = new JsonObject();
                category.add(catName, offers);
                defaultShop.add(category);
            }
            JsonObject root = new JsonObject();
            root.add(DEFAULT_SHOP_KEY, defaultShop);
            Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Saved default shop config to {}", file);
        } catch (Exception ex) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to save default shop config: {}", ex.getMessage());
        }
    }

    /**
     * @deprecated Use saveCategories()
     */
    public static void saveEntries(List<ShopEntryRecord> entries) {
        Map<String, List<ShopEntryRecord>> cats = new LinkedHashMap<>();
        cats.put(DEFAULT_CATEGORY, new ArrayList<>(entries));
        saveCategories(cats);
    }
}
