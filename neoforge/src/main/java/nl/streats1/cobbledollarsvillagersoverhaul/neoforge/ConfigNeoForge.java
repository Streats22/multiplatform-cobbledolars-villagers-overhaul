package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;

public class ConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** CobbleDollars steps: 1 = 250 CD, 2 = 500 CD, 3 = 750 CD per emerald. Use step 1-3. Custom values (e.g. 250, 500) also accepted. */
    public static final ModConfigSpec.IntValue COBBLEDOLLARS_EMERALD_RATE = BUILDER
            .comment("Step 1-3: 1=250, 2=500, 3=750 CD per emerald (CobbleDollars scale). Or set raw CD value (250, 500, 750). Ignored if syncCobbleDollarsBankRate is true.")
            .defineInRange("cobbledollarsEmeraldRate", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SYNC_COBBLEDOLLARS_BANK_RATE = BUILDER
            .comment("If true, use the emerald price from CobbleDollars' config/cobbledollars/bank.json so villager rate matches their bank (1 emerald = X). Falls back to cobbledollarsEmeraldRate if file not found.")
            .define("syncCobbleDollarsBankRate", true);

    public static final ModConfigSpec.BooleanValue VILLAGERS_ACCEPT_COBBLEDOLLARS = BUILDER
            .comment("If true, villager trades that cost emeralds can be paid with CobbleDollars balance instead.")
            .define("villagersAcceptCobbleDollars", true);

    public static final ModConfigSpec.BooleanValue USE_COBBLEDOLLARS_SHOP_UI = BUILDER
            .comment("Use CobbleDollars-style shop UI when trading with villagers.")
            .define("useCobbleDollarsShopUi", true);

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_CURRENCY_ITEMS = BUILDER
            .comment("Items that work like emeralds. 1 emerald = 250 CD. Format: [{\"item\":\"cobblemon:relic_coin\",\"value\":250}]. Value = CobbleDollars per 1 item. Trades with these as cost→BUY, as result→SELL. Empty = use config/cobbledollars_villagers_overhaul_rca/custom_currency.json.")
            .define("customCurrencyItems", "[{\"item\":\"cobblemon:relic_coin\",\"value\":250},{\"item\":\"cobblemon:relic_coin_pouch\",\"value\":2250},{\"item\":\"cobblemon:relic_coin_sack\",\"value\":20250},{\"item\":\"allthemons:token\",\"value\":250}]");

    static final ModConfigSpec SPEC = BUILDER.build();
    
    /** Called when config screen is saved - applies current values to Config and CustomCurrencyConfig. */
    public static void saveFromScreen() {
        loadConfig(SPEC);
    }

    private static int stepToCd(int raw) {
        return (raw >= 1 && raw <= 3) ? raw * 250 : raw;
    }

    public static void loadConfig(ModConfigSpec spec) {
        Config.setCobbledollarsEmeraldRate(stepToCd(COBBLEDOLLARS_EMERALD_RATE.get()));
        Config.setSyncCobbleDollarsBankRate(SYNC_COBBLEDOLLARS_BANK_RATE.get());
        Config.setVillagersAcceptCobbleDollars(VILLAGERS_ACCEPT_COBBLEDOLLARS.get());
        Config.setUseCobbleDollarsShopUi(USE_COBBLEDOLLARS_SHOP_UI.get());
        String customCurrency = CUSTOM_CURRENCY_ITEMS.get();
        nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig.setConfigOverride(
                customCurrency == null || customCurrency.isBlank() || "[]".equals(customCurrency.trim()) ? null : customCurrency);
        ItemPriceConfig.loadAndApply();
    }
}
