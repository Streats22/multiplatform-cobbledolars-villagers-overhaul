package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for auto-generated config files under
 * {@code config/cobbledollars_villagers_overhaul_rca/}.
 */
public final class ModConfigDefaults {

    /**
     * Default CobbleDollars per emerald in {@code cobbledollarsEmeraldRate} / {@code Config#COBBLEDOLLARS_EMERALD_RATE}.
     */
    public static final int DEFAULT_EMERALD_RATE_CD = 750;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModConfigDefaults() {
    }

    /**
     * {@code config/cobbledollars_villagers_overhaul_rca/config.json} (Fabric).
     * {@code cobbledollarsEmeraldRate} is literal CD per emerald (250 = 250 CD).
     */
    public static String fabricMainConfigJson() {
        return """
                {
                  "cobbledollarsEmeraldRate": %d,
                  "syncCobbleDollarsBankRate": true,
                  "villagersAcceptCobbleDollars": true,
                  "freeMinimumEmeraldTrade": false,
                  "useCobbleDollarsShopUi": true,
                  "useRctTradesOverhaul": true,
                  "useDatapackTrades": true,
                  "excludedVillagerProfessionNamespaces": [
                    "cobbledollars"
                  ],
                  "excludedVillagerProfessionIds": [
                    "casinorocket:casino_worker"
                  ],
                  "_comment_emeraldRate": "CobbleDollars per emerald (literal). Used for villager trades. Example: 250 = 250 CD.",
                  "_comment_syncBank": "Legacy field; villager rate always uses cobbledollarsEmeraldRate. Match that to bank.json emerald price if desired.",
                  "_comment_freeMinimum": "freeMinimumEmeraldTrade: 1-emerald trades (e.g. after curing) cost 0 CD when true.",
                  "_comment_excluded": "Excluded villagers keep their mod's native UI (not the CobbleDollars shop on right-click).",
                  "_comment_customCurrency": "Relic coins etc.: edit custom_currency.json in this folder."
                }
                """.formatted(DEFAULT_EMERALD_RATE_CD);
    }

    /**
     * Same as {@link #fabricMainConfigJson()} but with current values (Mod Menu save).
     */
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
        String excludedNsJson = jsonStringArray(excludedNamespaces);
        String excludedIdsJson = jsonStringArray(excludedProfessionIds);
        return """
                {
                  "cobbledollarsEmeraldRate": %d,
                  "syncCobbleDollarsBankRate": %s,
                  "villagersAcceptCobbleDollars": %s,
                  "freeMinimumEmeraldTrade": %s,
                  "useCobbleDollarsShopUi": %s,
                  "useRctTradesOverhaul": %s,
                  "useDatapackTrades": %s,
                  "excludedVillagerProfessionNamespaces": %s,
                  "excludedVillagerProfessionIds": %s,
                  "_comment_emeraldRate": "CobbleDollars per emerald (literal). Used for villager trades. Example: 250 = 250 CD.",
                  "_comment_syncBank": "Legacy field; villager rate always uses cobbledollarsEmeraldRate. Match that to bank.json emerald price if desired.",
                  "_comment_freeMinimum": "freeMinimumEmeraldTrade: 1-emerald trades (e.g. after curing) cost 0 CD when true.",
                  "_comment_excluded": "Excluded villagers keep their mod's native UI (not the CobbleDollars shop on right-click).",
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
                excludedNsJson,
                excludedIdsJson
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
