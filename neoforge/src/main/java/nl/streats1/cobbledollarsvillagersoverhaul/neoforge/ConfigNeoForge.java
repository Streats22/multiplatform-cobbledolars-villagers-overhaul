package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue COBBLEDOLLARS_EMERALD_RATE = BUILDER
            .comment("CobbleDollars per emerald (literal): used for villager emerald costs. Editing this updates trade CD prices.")
            .defineInRange("cobbledollarsEmeraldRate",
                    nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_EMERALD_RATE_CD,
                    1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SYNC_COBBLEDOLLARS_BANK_RATE = BUILDER
            .comment("Legacy toggle (kept for config compatibility). Villager emerald rate always uses cobbledollarsEmeraldRate; set it to match bank.json emerald price if your pack ties them.")
            .define("syncCobbleDollarsBankRate", true);

    public static final ModConfigSpec.BooleanValue VILLAGERS_ACCEPT_COBBLEDOLLARS = BUILDER
            .comment("If true, villager trades that cost emeralds can be paid with CobbleDollars balance instead.")
            .define("villagersAcceptCobbleDollars", true);

    public static final ModConfigSpec.BooleanValue FREE_MINIMUM_EMERALD_TRADE = BUILDER
            .comment("If true, trades that cost 1 emerald (minimum after curing/discounts) are free - no CobbleDollars charged. Simulates 'they won't charge an emerald' for saving villagers.")
            .define("freeMinimumEmeraldTrade", false);

    public static final ModConfigSpec.BooleanValue USE_COBBLEDOLLARS_SHOP_UI = BUILDER
            .comment("Use CobbleDollars-style shop UI when trading with villagers.")
            .define("useCobbleDollarsShopUi", true);

    public static final ModConfigSpec.BooleanValue USE_RCT_TRADES_OVERHAUL = BUILDER
            .comment("RCT trainer association: integrate trainer trades with the CobbleDollars shop when applicable.")
            .define("useRctTradesOverhaul", true);

    public static final ModConfigSpec.BooleanValue USE_DATAPACK_TRADES = BUILDER
            .comment("Price non-emerald datapack / custom item trades with CobbleDollars using item price tables.")
            .define("useDatapackTrades", true);

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_CURRENCY_ITEMS = BUILDER
            .comment("Custom currency items (JSON array). value = literal CobbleDollars per 1 item. Emeralds use cobbledollarsEmeraldRate — do not list minecraft:emerald here. Empty [] = use custom_currency.json.")
            .define("customCurrencyItems",
                    nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.neoForgeCustomCurrencyItemsDefault());

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = BUILDER
            .comment("Comma-separated mod namespaces whose villager professions use their native UI (e.g. cobbledollars). Empty = use CobbleDollars shop for them.")
            .define("excludedVillagerProfessionNamespaces", "cobbledollars");

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_VILLAGER_PROFESSION_IDS = BUILDER
            .comment("Comma-separated specific profession IDs to exclude (e.g. casinorocket:casino_worker). Use when only some professions from a mod have their own UI.")
            .define("excludedVillagerProfessionIds", "casinorocket:casino_worker");

    public static final ModConfigSpec.BooleanValue ENABLE_MCA_COMPATIBILITY = BUILDER
            .comment("If true and Minecraft Comes Alive is loaded, right-click stays with MCA's interaction GUI. Trade / shift-trade still open the CobbleDollars shop.")
            .define("enableMcaCompatibility", true);

    public static final ModConfigSpec.BooleanValue SKIP_SHOP_OVERRIDE_WHEN_SNEAKING = BUILDER
            .comment("If true, sneak-right-click does not open the shop (vanilla villager behaviour). Allows leads, Carry On, Sophisticated Backpacks pickup, Easy Villagers, etc.")
            .define("skipShopOverrideWhenSneaking", true);

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ENTITY_TYPE_NAMESPACES = BUILDER
            .comment("Comma-separated entity-type namespaces that keep their own interact (not the CobbleDollars shop). Empty = none.")
            .define("excludedEntityTypeNamespaces", "");

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ENTITY_TYPE_IDS = BUILDER
            .comment("Comma-separated entity-type ids (namespace:path) that keep their own interact. Empty = none.")
            .define("excludedEntityTypeIds", "");

    public static final ModConfigSpec.ConfigValue<String> PASSTHROUGH_INTERACT_ITEM_IDS = BUILDER
            .comment("Comma-separated held-item ids that must not open the shop (e.g. a specific lasso). Spawn eggs and named name tags always pass through.")
            .define("passthroughInteractItemIds", String.join(",", nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_PASSTHROUGH_INTERACT_ITEM_IDS));

    public static final ModConfigSpec.ConfigValue<String> PASSTHROUGH_INTERACT_ITEM_NAMESPACES = BUILDER
            .comment("Comma-separated item namespaces that must not open the shop (lassos, backpacks, catchers). Unknown mods are ignored.")
            .define("passthroughInteractItemNamespaces", String.join(",", nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.DEFAULT_PASSTHROUGH_INTERACT_ITEM_NAMESPACES));

    static final ModConfigSpec SPEC = BUILDER.build();
    
        public static void saveFromScreen() {
        loadConfig(SPEC);
    }

    public static void loadConfig(ModConfigSpec spec) {
        Config.setCobbledollarsEmeraldRate(
                nl.streats1.cobbledollarsvillagersoverhaul.integration.EmeraldRateHelper.normalizeCdPerEmerald(
                        COBBLEDOLLARS_EMERALD_RATE.get()));
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
        Config.setSyncCobbleDollarsBankRate(SYNC_COBBLEDOLLARS_BANK_RATE.get());
        Config.setVillagersAcceptCobbleDollars(VILLAGERS_ACCEPT_COBBLEDOLLARS.get());
        Config.setFreeMinimumEmeraldTrade(FREE_MINIMUM_EMERALD_TRADE.get());
        Config.setUseCobbleDollarsShopUi(USE_COBBLEDOLLARS_SHOP_UI.get());
        Config.setUseRctTradesOverhaul(USE_RCT_TRADES_OVERHAUL.get());
        Config.setUseDatapackTrades(USE_DATAPACK_TRADES.get());
        Config.setExcludedVillagerProfessionNamespaces(parseCommaSeparated(EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.get()));
        Config.setExcludedVillagerProfessionIds(parseCommaSeparated(EXCLUDED_VILLAGER_PROFESSION_IDS.get()));
        Config.setEnableMcaCompatibility(ENABLE_MCA_COMPATIBILITY.get());
        Config.setSkipShopOverrideWhenSneaking(SKIP_SHOP_OVERRIDE_WHEN_SNEAKING.get());
        Config.setExcludedEntityTypeNamespaces(parseCommaSeparated(EXCLUDED_ENTITY_TYPE_NAMESPACES.get()));
        Config.setExcludedEntityTypeIds(parseCommaSeparated(EXCLUDED_ENTITY_TYPE_IDS.get()));
        Config.setPassthroughInteractItemIds(parseCommaSeparated(PASSTHROUGH_INTERACT_ITEM_IDS.get()));
        Config.setPassthroughInteractItemNamespaces(parseCommaSeparated(PASSTHROUGH_INTERACT_ITEM_NAMESPACES.get()));
        String customCurrency = CUSTOM_CURRENCY_ITEMS.get();
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig.setConfigOverride(
                customCurrency == null || customCurrency.isBlank() || "[]".equals(customCurrency.trim()) ? null : customCurrency);
        ItemPriceConfig.loadAndApply();
    }

    private static List<String> parseCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
