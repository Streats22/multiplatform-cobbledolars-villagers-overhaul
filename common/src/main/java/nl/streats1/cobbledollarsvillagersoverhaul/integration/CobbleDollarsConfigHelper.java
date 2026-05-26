package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.util.JsonPriceParser;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ShopOfferEntryFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class CobbleDollarsConfigHelper {

    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";

    /**
     * Set config root (e.g. FabricLoader.getConfigDir()). Null = use default.
     * Delegates to {@link ModConfig#setConfigRoot(Path)}.
     */
    public static void setConfigRoot(Path path) {
        ModConfig.setConfigRoot(path);
    }
    private static final String BANK_FILE = "bank.json";
    private static final String DEFAULT_SHOP_FILE = "default_shop.json";
    private static final String EMERALD_ITEM = "minecraft:emerald";

    private static final String DEFAULT_SHOP_KEY = "defaultShop";
    private static final String MERCHANT_SHOP_KEY = "merchantShop";

    private static int cachedBankEmeraldPrice = -1;

    public static OptionalInt getBankEmeraldPrice() {
        if (cachedBankEmeraldPrice >= 0) return OptionalInt.of(cachedBankEmeraldPrice);
        if (cachedBankEmeraldPrice == -2) return OptionalInt.empty();
        Path configDir = getConfigDirectory();
        Path bankFile = configDir.resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(BANK_FILE);
        if (!Files.isRegularFile(bankFile)) {
            cachedBankEmeraldPrice = -2;
            return OptionalInt.empty();
        }
        try {
            String content = Files.readString(bankFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has("bank")) {
                cachedBankEmeraldPrice = -2;
                return OptionalInt.empty();
            }
            JsonElement bankEl = root.get("bank");
            if (bankEl.isJsonArray()) {
                JsonArray bank = bankEl.getAsJsonArray();
                for (JsonElement entry : bank) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject obj = entry.getAsJsonObject();
                    String item = obj.has("item") ? obj.get("item").getAsString() : null;
                    if (EMERALD_ITEM.equals(item) && obj.has("price")) {
                        int price = obj.get("price").getAsInt();
                        if (price > 0) {
                            cachedBankEmeraldPrice = price;
                            return OptionalInt.of(price);
                        }
                    }
                }
            }
            cachedBankEmeraldPrice = -2;
            return OptionalInt.empty();
        } catch (Exception e) {
            cachedBankEmeraldPrice = -2;
            return OptionalInt.empty();
        }
    }

    public static List<CobbleDollarsShopPayloads.ShopOfferEntry> getDefaultShopBuyOffers() {
        Path configDir = getConfigDirectory();
        Path shopFile = configDir.resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(DEFAULT_SHOP_FILE);
        if (!Files.isRegularFile(shopFile)) return List.of();
        try {
            String content = Files.readString(shopFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            List<CobbleDollarsShopPayloads.ShopOfferEntry> out = new ArrayList<>();

            if (root.has(DEFAULT_SHOP_KEY)) {
                JsonElement arrEl = root.get(DEFAULT_SHOP_KEY);
                if (arrEl.isJsonArray()) {
                    JsonArray categories = arrEl.getAsJsonArray();
                    for (JsonElement catEl : categories) {
                        if (!catEl.isJsonObject()) continue;
                        JsonObject catObj = catEl.getAsJsonObject();
                        for (String catName : catObj.keySet()) {
                            JsonElement offersEl = catObj.get(catName);
                            if (!offersEl.isJsonArray()) continue;
                            for (JsonElement offerEl : offersEl.getAsJsonArray()) {
                                if (!offerEl.isJsonObject()) continue;
                                JsonObject o = offerEl.getAsJsonObject();
                                String itemId = o.has("item") ? o.get("item").getAsString() : null;
                                if (itemId == null || itemId.isEmpty()) continue;
                                int price = JsonPriceParser.parse(o.get("price"));
                                if (price <= 0) continue;
                                ResourceLocation id = ResourceLocation.tryParse(itemId);
                                if (id == null) continue;
                                var item = BuiltInRegistries.ITEM.get(id);
                                if (item == Items.AIR) continue;
                                out.add(ShopOfferEntryFactory.buyConfig(new ItemStack(item, 1), price, catName != null ? catName : ""));
                            }
                        }
                    }
                }
            }
            if (out.isEmpty() && root.has(MERCHANT_SHOP_KEY)) {
                JsonObject merchantShop = root.getAsJsonObject(MERCHANT_SHOP_KEY);
                for (String category : merchantShop.keySet()) {
                    JsonElement catEl = merchantShop.get(category);
                    if (!catEl.isJsonObject()) continue;
                    for (String itemId : catEl.getAsJsonObject().keySet()) {
                        int price = JsonPriceParser.parse(catEl.getAsJsonObject().get(itemId));
                        if (price <= 0) continue;
                        ResourceLocation id = ResourceLocation.tryParse(itemId);
                        if (id == null) continue;
                        var item = BuiltInRegistries.ITEM.get(id);
                        if (item == Items.AIR) continue;
                        out.add(ShopOfferEntryFactory.buyConfig(new ItemStack(item, 1), price, category));
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Get bank sell offers from CobbleDollars bank.json.
     * Each entry: player sells item for price (CobbleDollars).
     * Format: { "bank": [ { "item": "id", "price": X }, ... ] }
     * ShopOfferEntry for sell: result = item player sells, emeraldCount = CD they receive.
     */
    public static List<CobbleDollarsShopPayloads.ShopOfferEntry> getBankSellOffers() {
        Path configDir = getConfigDirectory();
        Path bankFile = configDir.resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(BANK_FILE);
        if (!Files.isRegularFile(bankFile)) return List.of();
        try {
            String content = Files.readString(bankFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            List<CobbleDollarsShopPayloads.ShopOfferEntry> out = new ArrayList<>();
            if (!root.has("bank")) return List.of();
            JsonElement bankEl = root.get("bank");
            if (!bankEl.isJsonArray()) return List.of();
            JsonArray bank = bankEl.getAsJsonArray();
            for (JsonElement entry : bank) {
                if (!entry.isJsonObject()) continue;
                JsonObject obj = entry.getAsJsonObject();
                String itemId = obj.has("item") ? obj.get("item").getAsString() : null;
                if (itemId == null || itemId.isEmpty()) continue;
                int price = JsonPriceParser.parse(obj.get("price"));
                if (price <= 0) continue;
                ResourceLocation id = ResourceLocation.tryParse(itemId);
                if (id == null) continue;
                var item = BuiltInRegistries.ITEM.get(id);
                if (item == null || item == Items.AIR) continue;
                // Sell offer: result = item player gives, emeraldCount = CD they receive (directPrice=true)
                out.add(ShopOfferEntryFactory.sellDirect(new ItemStack(item, 1), price));
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    public static int getEffectiveEmeraldRate() {
        if (Config.SYNC_COBBLEDOLLARS_BANK_RATE) {
            OptionalInt bank = getBankEmeraldPrice();
            if (bank.isPresent()) return bank.getAsInt();
        }
        return Config.COBBLEDOLLARS_EMERALD_RATE;
    }

    /**
     * Config root (e.g. .minecraft/config). Used by CobbleDollars config files.
     */
    public static Path getConfigDirectory() {
        return ModConfig.getConfigDirectory();
    }
}
