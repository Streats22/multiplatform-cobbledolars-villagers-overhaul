package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class DatapackItemPricing {
    private static final Map<String, Integer> customPrices = new HashMap<>();
    private static boolean pricesLoaded = false;

        public static void loadCustomPrices(String jsonConfig) {
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            pricesLoaded = false;
            return;
        }

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Integer>>() {
            }.getType();
            Map<String, Integer> loaded = gson.fromJson(jsonConfig, type);

            if (loaded != null) {
                customPrices.clear();
                customPrices.putAll(loaded);
                pricesLoaded = true;
            }
        } catch (Exception e) {
        }
    }

        public static int getOverridePrice(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return 0;
        if (!pricesLoaded || customPrices.isEmpty()) return 0;
        int perItem = resolveCustomPrice(itemStack.getItem());
        return perItem > 0 ? perItem * itemStack.getCount() : 0;
    }

        public static int getPrice(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return 0;
        int perItem = resolveCustomPrice(itemStack.getItem());
        if (perItem > 0) return perItem * itemStack.getCount();
        return CobbleDollarsConfigHelper.getEffectiveEmeraldRate() * itemStack.getCount();
    }

        public static int getSingleItemPrice(Item item) {
        if (item == null) return 0;
        int perItem = resolveCustomPrice(item);
        return perItem > 0 ? perItem : CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
    }

        private static int resolveCustomPrice(Item item) {
        if (item == null) return 0;
        String path = getItemId(item);
        Integer byPath = customPrices.get(path);
        if (byPath != null) return byPath;
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        if (registryName == null) return 0;
        Integer byFull = customPrices.get(registryName.toString());
        return byFull != null ? byFull : 0;
    }

        private static String getItemId(Item item) {
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        return registryName != null ? registryName.getPath() : "";
    }

        public static void addCustomPrice(String itemId, int price) {
        customPrices.put(itemId.toLowerCase(), price);
        pricesLoaded = true;
    }

        public static boolean hasCustomPrices() {
        return pricesLoaded;
    }

        public static Map<String, Integer> getCustomPrices() {
        return new HashMap<>(customPrices);
    }
}
