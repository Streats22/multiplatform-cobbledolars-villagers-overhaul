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
        public static boolean FREE_MINIMUM_EMERALD_TRADE = false;
        public static List<String> EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = new ArrayList<>(List.of("cobbledollars"));
        public static List<String> EXCLUDED_VILLAGER_PROFESSION_IDS = new ArrayList<>(List.of("casinorocket:casino_worker"));
        public static boolean ENABLE_MCA_COMPATIBILITY = true;
        public static boolean SKIP_SHOP_OVERRIDE_WHEN_SNEAKING = true;
        public static List<String> EXCLUDED_ENTITY_TYPE_NAMESPACES = new ArrayList<>();
        public static List<String> EXCLUDED_ENTITY_TYPE_IDS = new ArrayList<>();
        public static List<String> PASSTHROUGH_INTERACT_ITEM_IDS = new ArrayList<>(
            nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_PASSTHROUGH_INTERACT_ITEM_IDS);
        public static List<String> PASSTHROUGH_INTERACT_ITEM_NAMESPACES = new ArrayList<>(
            nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_PASSTHROUGH_INTERACT_ITEM_NAMESPACES);
    public static int DATAPACK_ITEM_PRICE_RARITY_COMMON = 1;
    public static int DATAPACK_ITEM_PRICE_RARITY_UNCOMMON = 5;
    public static int DATAPACK_ITEM_PRICE_RARITY_RARE = 20;
    public static int DATAPACK_ITEM_PRICE_RARITY_EPIC = 50;
    public static int DATAPACK_ITEM_PRICE_RARITY_LEGENDARY = 125;

    public static void loadConfig() {
    }
    
    public static void setCobbledollarsEmeraldRate(int value) {
        COBBLEDOLLARS_EMERALD_RATE = Math.max(1, value);
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
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

    public static void setEnableMcaCompatibility(boolean value) {
        ENABLE_MCA_COMPATIBILITY = value;
    }

    public static void setSkipShopOverrideWhenSneaking(boolean value) {
        SKIP_SHOP_OVERRIDE_WHEN_SNEAKING = value;
    }

    public static void setExcludedEntityTypeNamespaces(List<String> list) {
        EXCLUDED_ENTITY_TYPE_NAMESPACES = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static void setExcludedEntityTypeIds(List<String> list) {
        EXCLUDED_ENTITY_TYPE_IDS = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static void setPassthroughInteractItemIds(List<String> list) {
        PASSTHROUGH_INTERACT_ITEM_IDS = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static void setPassthroughInteractItemNamespaces(List<String> list) {
        PASSTHROUGH_INTERACT_ITEM_NAMESPACES = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

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

        public static boolean isVillagerProfessionExcluded(String namespace) {
        return namespace != null && !EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.isEmpty()
                && EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.stream().anyMatch(ns -> ns != null && ns.equalsIgnoreCase(namespace));
    }

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
