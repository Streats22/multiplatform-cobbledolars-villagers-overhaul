package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable currency items that work like emeralds.
 * When a trade uses these items as cost or result, they are converted to/from CobbleDollars.
 * Supports: Cobblemon Relic Coins, Relic Coin Pouches/Sacks, Poketokens (All The Mons), etc.
 * - Trade "4 Apricorns → 1 Coin" → SELL tab (player sells apricorns, gets CobbleDollars)
 * - Trade "12 Coins → 1 Potion" → BUY tab (player pays CobbleDollars, gets potion)
 */
public final class CustomCurrencyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE = "custom_currency.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";

    /** Default currencies: emerald + Cobblemon Relic Coins (if Cobblemon present) + Poketokens (if All The Mons present). Only items from loaded mods are included. */
    private static String getDefaultJson() {
        List<CurrencyEntry> defaults = new ArrayList<>();
        defaults.add(entry("minecraft:emerald", 750));
        defaults.add(entry("cobblemon:relic_coin", 250));
        defaults.add(entry("cobblemon:relic_coin_pouch", 2250));
        defaults.add(entry("cobblemon:relic_coin_sack", 20250));
        defaults.add(entry("allthemons:token", 750));
        List<CurrencyEntry> filtered = defaults.stream()
                .filter(e -> e != null && e.item != null && isItemRegistered(e.item))
                .toList();
        return new Gson().toJson(filtered);
    }

    private static CurrencyEntry entry(String itemId, int value) {
        CurrencyEntry e = new CurrencyEntry();
        e.item = itemId;
        e.value = value;
        return e;
    }

    private static boolean isItemRegistered(String itemId) {
        try {
            String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
            ResourceLocation loc = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.get(loc);
            return item != null && item != Items.AIR;
        } catch (Exception e) {
            return false;
        }
    }

    /** itemId -> CobbleDollars value per 1 item */
    private static final Map<String, Integer> CURRENCY_VALUES = new HashMap<>();
    private static boolean loaded = false;
    /** Override from mod config (JSON string) - takes precedence over file */
    private static String configOverride = null;
    /** Optional config root (Fabric sets via FabricLoader.getConfigDir()) */
    private static Path configRootOverride = null;

    /** Set config directory (e.g. FabricLoader.getInstance().getConfigDir()). Null = use default. */
    public static void setConfigRoot(Path path) {
        configRootOverride = path;
    }

    public static void loadFromFile() {
        Path configDir = getConfigDirectory();
        Path dir = configDir.resolve(CONFIG_SUBDIR);
        Path file = dir.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(file)) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, getDefaultJson());
            } catch (Exception e) {
            }
            if (configOverride == null) loadFromJson(getDefaultJson());
            loaded = true;
            return;
        }
        try {
            String content = Files.readString(file);
            loadFromJson(content);
        } catch (Exception e) {
            LOGGER.warn("Failed to load custom currency config: {}", e.getMessage());
        }
        loaded = true;
    }

    public static void loadFromJson(String json) {
        if (json == null || json.isBlank()) {
            CURRENCY_VALUES.clear();
            loaded = true;
            return;
        }
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<CurrencyEntry>>() {}.getType();
            List<CurrencyEntry> entries = gson.fromJson(json.trim(), listType);
            CURRENCY_VALUES.clear();
            if (entries != null) {
                for (CurrencyEntry e : entries) {
                    if (e != null && e.item != null && !e.item.isEmpty()) {
                        String id = e.item.contains(":") ? e.item : "minecraft:" + e.item;
                        int val = e.value > 0 ? e.value : 750;
                        CURRENCY_VALUES.put(id.toLowerCase(), val);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse custom currency JSON: {}", e.getMessage());
        }
        loaded = true;
    }

    /** Set JSON override from mod config. Pass null to use file. */
    public static void setConfigOverride(String json) {
        configOverride = json;
        loaded = false;
    }

    /** Call before reading entries in UI to ensure config is loaded (e.g. from file or NeoForge). */
    public static void ensureLoadedForUi() {
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            if (configOverride != null) {
                loadFromJson(configOverride);
            } else {
                loadFromFile();
            }
        }
    }

    public static boolean isCurrencyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ensureLoaded();
        return getCurrencyValue(stack) > 0;
    }

    /** CobbleDollars value per single item. 0 if not a currency item. */
    public static int getCurrencyValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String fullId = id.toString().toLowerCase();
        if (CURRENCY_VALUES.containsKey(fullId)) return CURRENCY_VALUES.get(fullId);
        if (CURRENCY_VALUES.containsKey(id.getPath().toLowerCase())) return CURRENCY_VALUES.get(id.getPath().toLowerCase());
        return 0;
    }

    /** Total CobbleDollars value for the stack (count * value per item). */
    public static long getTotalValue(ItemStack stack) {
        int perItem = getCurrencyValue(stack);
        if (perItem <= 0) return 0;
        return (long) perItem * stack.getCount();
    }

    /** Get all currency entries for UI. Returns a copy. */
    public static List<CurrencyEntryRecord> getEntries() {
        ensureLoaded();
        List<CurrencyEntryRecord> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : CURRENCY_VALUES.entrySet()) {
            out.add(new CurrencyEntryRecord(e.getKey(), e.getValue()));
        }
        return out;
    }

    /** Replace all entries and persist. Used by config UI. */
    public static void replaceEntries(List<CurrencyEntryRecord> entries) {
        CURRENCY_VALUES.clear();
        for (CurrencyEntryRecord e : entries) {
            if (e != null && e.itemId() != null && !e.itemId().isEmpty()) {
                String id = e.itemId().contains(":") ? e.itemId() : "minecraft:" + e.itemId();
                int val = e.value() > 0 ? e.value() : 750;
                CURRENCY_VALUES.put(id.toLowerCase(), val);
            }
        }
        loaded = true;
    }

    /** Persist current entries to custom_currency.json. Call after replaceEntries when using file (Fabric). */
    public static void saveToFile() {
        if (configOverride != null) return; // NeoForge uses TOML; platform handles save
        try {
            Path dir = getConfigDirectory().resolve(CONFIG_SUBDIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(CONFIG_FILE);
            List<CurrencyEntry> list = new ArrayList<>();
            for (Map.Entry<String, Integer> e : CURRENCY_VALUES.entrySet()) {
                CurrencyEntry ce = new CurrencyEntry();
                ce.item = e.getKey();
                ce.value = e.getValue();
                list.add(ce);
            }
            String json = new Gson().toJson(list);
            Files.writeString(file, json);
        } catch (Exception ex) {
        }
    }

    /** Write entries to custom_currency.json. Call from GUI save so JSON stays in sync (Fabric + NeoForge). */
    public static void writeEntriesToFile(List<CurrencyEntryRecord> entries) {
        try {
            Path dir = getConfigDirectory().resolve(CONFIG_SUBDIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(CONFIG_FILE);
            Files.writeString(file, entriesToJson(entries));
        } catch (Exception ex) {
        }
    }

    /** Convert entries to JSON string. For NeoForge TOML. */
    public static String entriesToJson(List<CurrencyEntryRecord> entries) {
        List<CurrencyEntry> list = new ArrayList<>();
        for (CurrencyEntryRecord e : entries) {
            if (e != null && e.itemId() != null && !e.itemId().isEmpty()) {
                CurrencyEntry ce = new CurrencyEntry();
                ce.item = e.itemId().contains(":") ? e.itemId() : "minecraft:" + e.itemId();
                ce.value = e.value() > 0 ? e.value() : 750;
                list.add(ce);
            }
        }
        return new Gson().toJson(list);
    }

    private static Path getConfigDirectory() {
        if (configRootOverride != null) return configRootOverride;
        return Path.of("config").toAbsolutePath();
    }

    private static class CurrencyEntry {
        String item;
        int value;
    }
}
