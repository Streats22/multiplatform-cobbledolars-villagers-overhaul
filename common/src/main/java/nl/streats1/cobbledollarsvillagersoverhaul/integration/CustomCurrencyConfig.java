package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomCurrencyConfig {
    private static final String CONFIG_FILE = "custom_currency.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";

    private static String getDefaultJson() {
        return ModConfigDefaults.customCurrencyJson();
    }

    public static boolean isItemIdRegistered(String itemId) {
        try {
            String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
            ResourceLocation loc = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.get(loc);
            return item != null && item != Items.AIR;
        } catch (Exception e) {
            return false;
        }
    }

    private static final Map<String, Integer> CURRENCY_VALUES = new HashMap<>();
    private static boolean loaded = false;
    private static String configOverride = null;

    public static void setConfigRoot(Path path) {
        ModConfig.setConfigRoot(path);
    }

    public static void loadFromFile() {
        Path configDir = ModConfig.getConfigDirectory();
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
                        if ("minecraft:emerald".equalsIgnoreCase(id)) {
                            continue;
                        }
                        int val = e.value > 0 ? e.value : ModConfigDefaults.DEFAULT_EMERALD_RATE_CD;
                        CURRENCY_VALUES.put(id.toLowerCase(), val);
                    }
                }
            }
        } catch (Exception e) {
        }
        loaded = true;
    }

    public static void setConfigOverride(String json) {
        configOverride = json;
        loaded = false;
    }

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

    public static int getCurrencyValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.is(Items.EMERALD)) {
            return CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        }
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String fullId = id.toString().toLowerCase();
        if (CURRENCY_VALUES.containsKey(fullId)) return CURRENCY_VALUES.get(fullId);
        if (CURRENCY_VALUES.containsKey(id.getPath().toLowerCase())) return CURRENCY_VALUES.get(id.getPath().toLowerCase());
        return 0;
    }

    public static long getTotalValue(ItemStack stack) {
        int perItem = getCurrencyValue(stack);
        if (perItem <= 0) return 0;
        return (long) perItem * stack.getCount();
    }

    public static List<CurrencyEntryRecord> getEntries() {
        ensureLoaded();
        List<CurrencyEntryRecord> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : CURRENCY_VALUES.entrySet()) {
            out.add(new CurrencyEntryRecord(e.getKey(), e.getValue()));
        }
        return out;
    }

    public static void replaceEntries(List<CurrencyEntryRecord> entries) {
        CURRENCY_VALUES.clear();
        for (CurrencyEntryRecord e : entries) {
            if (e != null && e.itemId() != null && !e.itemId().isEmpty()) {
                String id = e.itemId().contains(":") ? e.itemId() : "minecraft:" + e.itemId();
                int val = e.value() > 0 ? e.value() : ModConfigDefaults.DEFAULT_EMERALD_RATE_CD;
                CURRENCY_VALUES.put(id.toLowerCase(), val);
            }
        }
        loaded = true;
    }

    public static void saveToFile() {
        if (configOverride != null) return; 
        try {
            Path dir = ModConfig.getConfigDirectory().resolve(CONFIG_SUBDIR);
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

    public static void writeEntriesToFile(List<CurrencyEntryRecord> entries) {
        try {
            Path dir = ModConfig.getConfigDirectory().resolve(CONFIG_SUBDIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(CONFIG_FILE);
            Files.writeString(file, entriesToJson(entries));
        } catch (Exception ex) {
        }
    }

    public static String entriesToJson(List<CurrencyEntryRecord> entries) {
        List<CurrencyEntry> list = new ArrayList<>();
        for (CurrencyEntryRecord e : entries) {
            if (e != null && e.itemId() != null && !e.itemId().isEmpty()) {
                CurrencyEntry ce = new CurrencyEntry();
                ce.item = e.itemId().contains(":") ? e.itemId() : "minecraft:" + e.itemId();
                ce.value = e.value() > 0 ? e.value() : ModConfigDefaults.DEFAULT_EMERALD_RATE_CD;
                list.add(ce);
            }
        }
        return new Gson().toJson(list);
    }

    private static class CurrencyEntry {
        String item;
        int value;
    }
}
