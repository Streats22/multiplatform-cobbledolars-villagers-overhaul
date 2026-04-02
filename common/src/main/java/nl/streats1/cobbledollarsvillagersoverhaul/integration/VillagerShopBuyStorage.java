package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

/**
 * Per-villager buy-tab data: {@code config/&lt;modId&gt;/villager_shop_data/&lt;uuid&gt;.json}
 * (same JSON shape as CobbleDollars {@code default_shop.json}).
 */
public final class VillagerShopBuyStorage {

    private static final String DEFAULT_SHOP_KEY = "defaultShop";

    private VillagerShopBuyStorage() {
    }

    private static Path dataDir() {
        Path root = VillagerShopConfig.getConfigRootForPath();
        return root.resolve(CobbleDollarsVillagersOverhaulRca.MOD_ID).resolve("villager_shop_data");
    }

    private static Path fileFor(UUID uuid) {
        return dataDir().resolve(uuid + ".json");
    }

    /**
     * If a per-villager file exists, load it; otherwise fall back to global default shop offers.
     */
    public static List<CobbleDollarsShopPayloads.ShopOfferEntry> getBuyOffersOrDefault(UUID villagerUuid) {
        Path f = fileFor(villagerUuid);
        if (!Files.isRegularFile(f)) {
            return CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
        }
        List<CobbleDollarsShopPayloads.ShopOfferEntry> parsed = CobbleDollarsConfigHelper.parseDefaultShopBuyOffersFromPath(f);
        return parsed != null ? parsed : List.of();
    }

    /**
     * Writes buy offers for this villager (CobbleDollars {@code defaultShop} format).
     */
    public static void saveBuyOffers(UUID villagerUuid, List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers) {
        Map<String, List<ShopEntryRecord>> categories = new LinkedHashMap<>();
        for (CobbleDollarsShopPayloads.ShopOfferEntry e : buyOffers) {
            if (e == null || e.result() == null || e.result().isEmpty()) continue;
            String cat = (e.categoryName() != null && !e.categoryName().isEmpty()) ? e.categoryName() : "Buy";
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(e.result().getItem());
            if (itemId == null) continue;
            int cdPrice = e.directPrice()
                    ? e.emeraldCount()
                    : e.emeraldCount() * CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            if (cdPrice <= 0) continue;
            categories.computeIfAbsent(cat, k -> new ArrayList<>())
                    .add(new ShopEntryRecord(itemId.toString(), cdPrice));
        }
        Path path = fileFor(villagerUuid);
        try {
            Files.createDirectories(path.getParent());
            JsonArray defaultShop = new JsonArray();
            for (Map.Entry<String, List<ShopEntryRecord>> entry : categories.entrySet()) {
                String catName = entry.getKey();
                if (catName == null || catName.isBlank()) continue;
                JsonArray offers = new JsonArray();
                for (ShopEntryRecord rec : entry.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("item", rec.itemId());
                    o.addProperty("price", rec.price());
                    offers.add(o);
                }
                JsonObject category = new JsonObject();
                category.add(catName, offers);
                defaultShop.add(category);
            }
            JsonObject root = new JsonObject();
            root.add(DEFAULT_SHOP_KEY, defaultShop);
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Saved villager shop buy data to {}", path);
        } catch (Exception ex) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to save villager shop buy data: {}", ex.getMessage());
        }
    }

    public static void deleteIfPresent(UUID villagerUuid) {
        try {
            Path path = fileFor(villagerUuid);
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
