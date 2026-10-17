package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * What villagers pay per item, in literal CobbleDollars. Any price from 1 up is allowed (no 250/750 steps).
 * An entry overrides the emerald-derived price of an existing sell trade, and adds a sell entry
 * for items the villager has no trade for.
 */
public final class SellPriceConfig {
    public static final int MIN_PRICE = 1;
    public static final int MAX_PRICE = 999_999;

    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";
    private static final String FILE = "villager_buy_prices.json";
    private static final String PRICES_KEY = "itemPrices";

    private static volatile Map<String, Integer> prices = Map.of();

    private SellPriceConfig() {
    }

    private static Path getConfigFile() {
        return CobbleDollarsConfigHelper.getConfigDirectory().resolve(CONFIG_SUBDIR).resolve(FILE);
    }

    public static int clampPrice(int price) {
        return Math.max(MIN_PRICE, Math.min(MAX_PRICE, price));
    }

    /** Keeps only well-formed {@code namespace:path} ids with a positive price. */
    public static Map<String, Integer> sanitize(Map<String, Integer> raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (raw == null) return out;
        for (Map.Entry<String, Integer> e : raw.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() < MIN_PRICE) continue;
            String id = e.getKey().trim().toLowerCase();
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            if (ResourceLocation.tryParse(id) == null) continue;
            out.put(id, clampPrice(e.getValue()));
        }
        return out;
    }

    public static Map<String, Integer> loadEntries() {
        Path file = getConfigFile();
        if (!Files.isRegularFile(file)) return new LinkedHashMap<>();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            Map<String, Integer> raw = new LinkedHashMap<>();
            JsonElement el = root.get(PRICES_KEY);
            if (el != null && el.isJsonObject()) {
                for (String key : el.getAsJsonObject().keySet()) {
                    try {
                        raw.put(key, el.getAsJsonObject().get(key).getAsInt());
                    } catch (Exception ignored) {
                    }
                }
            }
            return sanitize(raw);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    public static void loadAndApply() {
        apply(loadEntries());
    }

    public static void apply(Map<String, Integer> entries) {
        prices = Collections.unmodifiableMap(new TreeMap<>(sanitize(entries)));
    }

    public static void saveEntries(Map<String, Integer> entries) {
        Map<String, Integer> clean = sanitize(entries);
        try {
            Path file = getConfigFile();
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, Integer> e : clean.entrySet()) obj.addProperty(e.getKey(), e.getValue());
            JsonObject root = new JsonObject();
            root.add(PRICES_KEY, obj);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
        apply(clean);
    }

    /** Configured CobbleDollars per single unit, or 0 when this item has no configured price. */
    public static int unitPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty() || prices.isEmpty()) return 0;
        return unitPrice(stack.getItem());
    }

    public static int unitPrice(Item item) {
        if (item == null || prices.isEmpty()) return 0;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        Integer p = key == null ? null : prices.get(key.toString());
        return p == null ? 0 : p;
    }

    /** Configured entries in a stable order, shared by shop listing and sell handling so indices line up. */
    public static Map<String, Integer> all() {
        return prices;
    }

    public static Item resolve(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return Items.AIR;
        return BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.AIR);
    }
}
