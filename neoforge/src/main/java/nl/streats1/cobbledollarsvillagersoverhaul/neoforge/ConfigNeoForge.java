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

    static final ModConfigSpec SPEC = BUILDER.build();
    
    /** Called when config screen is saved - applies current values to Config and CustomCurrencyConfig. */
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
        String excludedNs = EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.get();
        List<String> excludedNamespaces = (excludedNs == null || excludedNs.isBlank())
                ? List.of()
                : Arrays.stream(excludedNs.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        Config.setExcludedVillagerProfessionNamespaces(excludedNamespaces);
        String excludedIds = EXCLUDED_VILLAGER_PROFESSION_IDS.get();
        List<String> excludedProfessionIds = (excludedIds == null || excludedIds.isBlank())
                ? List.of()
                : Arrays.stream(excludedIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        Config.setExcludedVillagerProfessionIds(excludedProfessionIds);
        String customCurrency = CUSTOM_CURRENCY_ITEMS.get();
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig.setConfigOverride(
                customCurrency == null || customCurrency.isBlank() || "[]".equals(customCurrency.trim()) ? null : customCurrency);
        ItemPriceConfig.loadAndApply();
    }
}
