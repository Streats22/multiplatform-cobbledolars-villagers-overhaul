package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public final class ModConfigDefaults {

        public static final int DEFAULT_EMERALD_RATE_CD = 750;

        public static final List<String> DEFAULT_PASSTHROUGH_INTERACT_ITEM_IDS = List.of();

        public static final List<String> DEFAULT_PASSTHROUGH_INTERACT_ITEM_NAMESPACES = List.of(
            "sophisticatedbackpacks",
            "easyvillagers",
            "moblassos",
            "lasso",
            "moblasso",
            "mob_lasso",
            "mobcatcher",
            "carryon",
            "cyclic"
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModConfigDefaults() {
    }

        public static String fabricMainConfigJson() {
        return fabricMainConfigJson(
                DEFAULT_EMERALD_RATE_CD,
                true,
                true,
                false,
                true,
                true,
                true,
                List.of("cobbledollars"),
                List.of("casinorocket:casino_worker"),
                true,
                true,
                List.of(),
                List.of(),
                DEFAULT_PASSTHROUGH_INTERACT_ITEM_IDS,
                DEFAULT_PASSTHROUGH_INTERACT_ITEM_NAMESPACES
        );
    }

        public static String fabricMainConfigJson(
            int cobbledollarsEmeraldRate,
            boolean syncCobbleDollarsBankRate,
            boolean villagersAcceptCobbleDollars,
            boolean freeMinimumEmeraldTrade,
            boolean useCobbleDollarsShopUi,
            boolean useRctTradesOverhaul,
            boolean useDatapackTrades,
            java.util.Collection<String> excludedNamespaces,
            java.util.Collection<String> excludedProfessionIds
    ) {
        return fabricMainConfigJson(
                cobbledollarsEmeraldRate,
                syncCobbleDollarsBankRate,
                villagersAcceptCobbleDollars,
                freeMinimumEmeraldTrade,
                useCobbleDollarsShopUi,
                useRctTradesOverhaul,
                useDatapackTrades,
                excludedNamespaces,
                excludedProfessionIds,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.ENABLE_MCA_COMPATIBILITY,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.SKIP_SHOP_OVERRIDE_WHEN_SNEAKING,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.EXCLUDED_ENTITY_TYPE_NAMESPACES,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.EXCLUDED_ENTITY_TYPE_IDS,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.PASSTHROUGH_INTERACT_ITEM_IDS,
                nl.streats1.cobbledollarsvillagersoverhaul.Config.PASSTHROUGH_INTERACT_ITEM_NAMESPACES
        );
    }

    public static String fabricMainConfigJson(
            int cobbledollarsEmeraldRate,
            boolean syncCobbleDollarsBankRate,
            boolean villagersAcceptCobbleDollars,
            boolean freeMinimumEmeraldTrade,
            boolean useCobbleDollarsShopUi,
            boolean useRctTradesOverhaul,
            boolean useDatapackTrades,
            java.util.Collection<String> excludedNamespaces,
            java.util.Collection<String> excludedProfessionIds,
            boolean enableMcaCompatibility,
            boolean skipShopOverrideWhenSneaking,
            java.util.Collection<String> excludedEntityTypeNamespaces,
            java.util.Collection<String> excludedEntityTypeIds,
            java.util.Collection<String> passthroughInteractItemIds,
            java.util.Collection<String> passthroughInteractItemNamespaces
    ) {
        return """
                {
                  "cobbledollarsEmeraldRate": %d,
                  "syncCobbleDollarsBankRate": %s,
                  "villagersAcceptCobbleDollars": %s,
                  "freeMinimumEmeraldTrade": %s,
                  "useCobbleDollarsShopUi": %s,
                  "useRctTradesOverhaul": %s,
                  "useDatapackTrades": %s,
                  "enableMcaCompatibility": %s,
                  "skipShopOverrideWhenSneaking": %s,
                  "excludedVillagerProfessionNamespaces": %s,
                  "excludedVillagerProfessionIds": %s,
                  "excludedEntityTypeNamespaces": %s,
                  "excludedEntityTypeIds": %s,
                  "passthroughInteractItemIds": %s,
                  "passthroughInteractItemNamespaces": %s,
                  "_comment_emeraldRate": "CobbleDollars per emerald (literal). Used for villager trades. Example: 250 = 250 CD.",
                  "_comment_syncBank": "Legacy field; villager rate always uses cobbledollarsEmeraldRate. Match that to bank.json emerald price if desired.",
                  "_comment_freeMinimum": "freeMinimumEmeraldTrade: 1-emerald trades (e.g. after curing) cost 0 CD when true.",
                  "_comment_excluded": "Excluded villagers keep their mod's native UI (not the CobbleDollars shop on right-click).",
                  "_comment_interact": "skipShopOverrideWhenSneaking matches vanilla (sneak does not trade). Passthrough item namespaces keep lassos/backpacks/catchers from opening the shop.",
                  "_comment_mca": "enableMcaCompatibility: right-click stays with MCA's GUI; Trade or shift-click opens this shop.",
                  "_comment_customCurrency": "Relic coins etc.: edit custom_currency.json in this folder."
                }
                """.formatted(
                cobbledollarsEmeraldRate,
                syncCobbleDollarsBankRate,
                villagersAcceptCobbleDollars,
                freeMinimumEmeraldTrade,
                useCobbleDollarsShopUi,
                useRctTradesOverhaul,
                useDatapackTrades,
                enableMcaCompatibility,
                skipShopOverrideWhenSneaking,
                jsonStringArray(excludedNamespaces),
                jsonStringArray(excludedProfessionIds),
                jsonStringArray(excludedEntityTypeNamespaces),
                jsonStringArray(excludedEntityTypeIds),
                jsonStringArray(passthroughInteractItemIds),
                jsonStringArray(passthroughInteractItemNamespaces)
        );
    }

    private static String jsonStringArray(java.util.Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String s : values) {
            if (!first) sb.append(", ");
            first = false;
            sb.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * {@code config/cobbledollars_villagers_overhaul_rca/custom_currency.json}.
     * Emeralds use {@link CobbleDollarsConfigHelper#getEffectiveEmeraldRate()} — not listed here.
     */
    public static String customCurrencyJson() {
        List<CurrencyEntry> entries = new ArrayList<>();
        entries.add(currencyEntry("cobblemon:relic_coin", 250));
        entries.add(currencyEntry("cobblemon:relic_coin_pouch", 2250));
        entries.add(currencyEntry("cobblemon:relic_coin_sack", 20250));
        entries.add(currencyEntry("allthemons:token", 250));
        return GSON.toJson(entries.stream()
                .filter(e -> CustomCurrencyConfig.isItemIdRegistered(e.item))
                .toList());
    }

    /**
     * NeoForge {@code customCurrencyItems} TOML default (compact JSON array).
     */
    public static String neoForgeCustomCurrencyItemsDefault() {
        return "[{\"item\":\"cobblemon:relic_coin\",\"value\":250},{\"item\":\"cobblemon:relic_coin_pouch\",\"value\":2250},{\"item\":\"cobblemon:relic_coin_sack\",\"value\":20250},{\"item\":\"allthemons:token\",\"value\":250}]";
    }

    private static CurrencyEntry currencyEntry(String itemId, int valueCd) {
        CurrencyEntry e = new CurrencyEntry();
        e.item = itemId;
        e.value = valueCd;
        return e;
    }

    private static class CurrencyEntry {
        String item;
        int value;
    }
}
