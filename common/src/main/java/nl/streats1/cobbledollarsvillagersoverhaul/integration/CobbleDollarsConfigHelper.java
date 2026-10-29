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
        for (BankEntryRecord entry : BankConfig.loadEntries()) {
            if (EMERALD_ITEM.equals(entry.itemId())) {
                int price = EmeraldRateHelper.normalizeCdPerEmerald(entry.price());
                if (price > 0) {
                    cachedBankEmeraldPrice = price;
                    return OptionalInt.of(price);
                }
            }
        }
        cachedBankEmeraldPrice = -2;
        return OptionalInt.empty();
    }

    public static List<CobbleDollarsShopPayloads.ShopOfferEntry> getDefaultShopBuyOffers() {
        Path configDir = getConfigDirectory();
        Path shopFile = configDir.resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(DEFAULT_SHOP_FILE);
        if (!Files.isRegularFile(shopFile)) return List.of();
        try {
            String content = Files.readString(shopFile);
            JsonObject root = DefaultShopConfig.asLegacyRoot(JsonParser.parseString(content));
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

    public static List<CobbleDollarsShopPayloads.ShopOfferEntry> getBankSellOffers() {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> out = new ArrayList<>();
        for (BankEntryRecord entry : BankConfig.loadEntries()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.itemId());
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.get(id);
            if (item == null || item == Items.AIR) continue;
            out.add(ShopOfferEntryFactory.sellDirect(new ItemStack(item, 1), entry.price()));
        }
        return out;
    }

    public static int getEffectiveEmeraldRate() {
        return Math.max(1, Config.COBBLEDOLLARS_EMERALD_RATE);
    }

    public static void invalidateBankEmeraldPriceCache() {
        cachedBankEmeraldPrice = -1;
    }

    public static Path getConfigDirectory() {
        return ModConfig.getConfigDirectory();
    }
}
