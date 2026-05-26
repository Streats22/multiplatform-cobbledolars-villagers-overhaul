package nl.streats1.cobbledollarsvillagersoverhaul;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static int COBBLEDOLLARS_EMERALD_RATE = nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_EMERALD_RATE_CD;
    public static boolean SYNC_COBBLEDOLLARS_BANK_RATE = true;
    public static boolean VILLAGERS_ACCEPT_COBBLEDOLLARS = true;
    public static boolean USE_COBBLEDOLLARS_SHOP_UI = true;
    public static boolean USE_RCT_TRADES_OVERHAUL = true;
    public static boolean USE_DATAPACK_TRADES = true;
    /** When true, trades that cost 1 emerald (minimum after curing/discounts) are free - no CobbleDollars charged. */
    public static boolean FREE_MINIMUM_EMERALD_TRADE = false;
    /**
     * Mod namespaces to exclude from CobbleDollars shop (e.g. cobbledollars for CobbleMerchant).
     */
    public static List<String> EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = new ArrayList<>(List.of("cobbledollars"));
    /**
     * Specific profession IDs to exclude (e.g. casinorocket:casino_worker for Casino Worker with Chip Table).
     * Use this when only some professions from a mod have their own UI, so others can use our shop.
     */
    public static List<String> EXCLUDED_VILLAGER_PROFESSION_IDS = new ArrayList<>(List.of("casinorocket:casino_worker"));
    public static int DATAPACK_ITEM_PRICE_RARITY_COMMON = 1;
    public static int DATAPACK_ITEM_PRICE_RARITY_UNCOMMON = 5;
    public static int DATAPACK_ITEM_PRICE_RARITY_RARE = 20;
    public static int DATAPACK_ITEM_PRICE_RARITY_EPIC = 50;
    public static int DATAPACK_ITEM_PRICE_RARITY_LEGENDARY = 125;

    public static void loadConfig() {
    }
    
    public static void setCobbledollarsEmeraldRate(int value) {
        COBBLEDOLLARS_EMERALD_RATE = Math.max(1, value);
    }
    
    public static void setSyncCobbleDollarsBankRate(boolean value) {
        SYNC_COBBLEDOLLARS_BANK_RATE = value;
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
    }
    
    public static void setVillagersAcceptCobbleDollars(boolean value) {
        VILLAGERS_ACCEPT_COBBLEDOLLARS = value;
    }
    
    public static void setUseCobbleDollarsShopUi(boolean value) {
        USE_COBBLEDOLLARS_SHOP_UI = value;
    }

    public static void setUseRctTradesOverhaul(boolean value) {
        USE_RCT_TRADES_OVERHAUL = value;
    }

    public static void setUseDatapackTrades(boolean value) {
        USE_DATAPACK_TRADES = value;
    }

    public static void setFreeMinimumEmeraldTrade(boolean value) {
        FREE_MINIMUM_EMERALD_TRADE = value;
    }

    public static void setExcludedVillagerProfessionNamespaces(List<String> list) {
        EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static void setExcludedVillagerProfessionIds(List<String> list) {
        EXCLUDED_VILLAGER_PROFESSION_IDS = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    /**
     * Applies shop-related booleans from the server (multiplayer). Client local config files are not used
     * for the authoritative server when connected; this keeps client behavior aligned with the dedicated server.
     */
    public static void applyServerShopRuntimeConfig(boolean useCobbleDollarsShopUi, boolean villagersAcceptCobbleDollars,
                                                    boolean useDatapackTrades, boolean useRctTradesOverhaul) {
        applyServerShopRuntimeConfig(useCobbleDollarsShopUi, villagersAcceptCobbleDollars, useDatapackTrades,
                useRctTradesOverhaul, COBBLEDOLLARS_EMERALD_RATE, SYNC_COBBLEDOLLARS_BANK_RATE);
    }

    public static void applyServerShopRuntimeConfig(boolean useCobbleDollarsShopUi, boolean villagersAcceptCobbleDollars,
                                                    boolean useDatapackTrades, boolean useRctTradesOverhaul,
                                                    int emeraldRateCdPerEmerald, boolean syncCobbleDollarsBankRate) {
        USE_COBBLEDOLLARS_SHOP_UI = useCobbleDollarsShopUi;
        VILLAGERS_ACCEPT_COBBLEDOLLARS = villagersAcceptCobbleDollars;
        USE_DATAPACK_TRADES = useDatapackTrades;
        USE_RCT_TRADES_OVERHAUL = useRctTradesOverhaul;
        COBBLEDOLLARS_EMERALD_RATE = Math.max(1, emeraldRateCdPerEmerald);
        SYNC_COBBLEDOLLARS_BANK_RATE = syncCobbleDollarsBankRate;
    }

    /** Check by namespace (all professions from that mod). */
    public static boolean isVillagerProfessionExcluded(String namespace) {
        return namespace != null && !EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.isEmpty()
                && EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.stream().anyMatch(ns -> ns != null && ns.equalsIgnoreCase(namespace));
    }

    /** Check by full profession ID (e.g. casinorocket:casino_worker) or namespace. */
    public static boolean isVillagerProfessionExcluded(ResourceLocation profId) {
        if (profId == null) return false;
        String fullId = profId.getNamespace() + ":" + profId.getPath();
        if (!EXCLUDED_VILLAGER_PROFESSION_IDS.isEmpty()
                && EXCLUDED_VILLAGER_PROFESSION_IDS.stream().anyMatch(id -> id != null && id.equalsIgnoreCase(fullId))) {
            return true;
        }
        return isVillagerProfessionExcluded(profId.getNamespace());
    }
}
