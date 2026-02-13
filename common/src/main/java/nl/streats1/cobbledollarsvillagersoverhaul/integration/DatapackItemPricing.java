package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple pricing system for CobbleDollars shop
 * Uses override prices for all items - much simpler and more maintainable
 */
public class DatapackItemPricing {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<String, Integer> customPrices = new HashMap<>();
    private static boolean pricesLoaded = false;

    /**
     * Initialize the pricing system with custom prices from config
     *
     * @param jsonConfig JSON string containing custom item prices in format: {"minecraft:diamond": 100, "minecraft:iron_ingot": 10}
     */
    public static void loadCustomPrices(String jsonConfig) {
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            LOGGER.info("No custom item prices provided, using default pricing");
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
                LOGGER.info("Loaded {} custom item prices", loaded.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse custom item prices: {}", e.getMessage());
        }
    }

    /**
     * Get the CobbleDollars value for an item stack
     * Priority: Override prices (highest priority)
     */
    public static int getPrice(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return 0;
        }

        Item item = itemStack.getItem();
        String itemId = getItemId(item);

        // Check override prices first (highest priority)
        if (customPrices.containsKey(itemId)) {
            return customPrices.get(itemId) * itemStack.getCount();
        }

        // Check with namespace:id format for override prices
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        String fullId = registryName.toString();
        if (customPrices.containsKey(fullId)) {
            return customPrices.get(fullId) * itemStack.getCount();
        }

        // Default fallback price - much more reasonable for economy balance
        return 1;
    }

    /**
     * Get price for a single item (not multiplied by count)
     * Used for configuration UI and price overrides
     */
    public static int getSingleItemPrice(Item item) {
        if (item == null) {
            return 0;
        }

        String itemId = getItemId(item);

        // Check override prices first
        if (customPrices.containsKey(itemId)) {
            return customPrices.get(itemId);
        }

        // Default fallback price - adjusted to be higher as requested
        return 2;
    }

    /**
     * Add a custom override price for an item
     * Useful for server commands or admin configuration
     * Simplified check - returns false to avoid version compatibility issues
     */
    private static boolean hasSpecialProperties(ItemStack itemStack) {
        // Simplified - assume no special properties for pricing purposes
        // This could be enhanced with version-specific NBT checks if needed
        return false;
    }

    /**
     * Get the simple item ID (just the path, not the namespace)
     */
    private static String getItemId(Item item) {
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        return registryName != null ? registryName.getPath() : "";
    }

    /**
     * Add a custom price for an item
     */
    public static void addCustomPrice(String itemId, int price) {
        customPrices.put(itemId.toLowerCase(), price);
        pricesLoaded = true;
    }

    /**
     * Check if custom prices are loaded
     */
    public static boolean hasCustomPrices() {
        return pricesLoaded;
    }

    /**
     * Get all custom prices
     */
    public static Map<String, Integer> getCustomPrices() {
        return new HashMap<>(customPrices);
    }
}
