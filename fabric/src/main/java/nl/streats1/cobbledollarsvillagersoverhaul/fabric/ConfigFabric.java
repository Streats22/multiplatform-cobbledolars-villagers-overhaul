package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric config loader. Creates config/cobbledollars_villagers_overhaul_rca/config.json
 * with defaults if missing. Custom currencies load from custom_currency.json (auto-created by CustomCurrencyConfig).
 */
public final class ConfigFabric {
    private static final String CONFIG_FILE = "config.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";

    public static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        CustomCurrencyConfig.setConfigRoot(configDir);
        Path dir = configDir.resolve(CONFIG_SUBDIR);
        Path file = dir.resolve(CONFIG_FILE);

        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(dir);
                String defaultJson = getDefaultConfigJson();
                Files.writeString(file, defaultJson);
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Created default config at {}", file);
            }

            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("cobbledollarsEmeraldRate")) {
                Config.setCobbledollarsEmeraldRate(stepToCd(root.get("cobbledollarsEmeraldRate").getAsInt()));
            }
            if (root.has("syncCobbleDollarsBankRate")) {
                Config.setSyncCobbleDollarsBankRate(root.get("syncCobbleDollarsBankRate").getAsBoolean());
            }
            if (root.has("villagersAcceptCobbleDollars")) {
                Config.setVillagersAcceptCobbleDollars(root.get("villagersAcceptCobbleDollars").getAsBoolean());
            }
            if (root.has("useCobbleDollarsShopUi")) {
                Config.setUseCobbleDollarsShopUi(root.get("useCobbleDollarsShopUi").getAsBoolean());
            }
            if (root.has("useRctTradesOverhaul")) {
                Config.setUseRctTradesOverhaul(root.get("useRctTradesOverhaul").getAsBoolean());
            }
            if (root.has("useDatapackTrades")) {
                Config.setUseDatapackTrades(root.get("useDatapackTrades").getAsBoolean());
            }
            if (root.has("excludedVillagerProfessionNamespaces") && root.get("excludedVillagerProfessionNamespaces").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("excludedVillagerProfessionNamespaces");
                java.util.List<String> list = new java.util.ArrayList<>();
                for (JsonElement el : arr) {
                    if (el.isJsonPrimitive()) list.add(el.getAsString());
                }
                Config.setExcludedVillagerProfessionNamespaces(list);
            }

            // Fabric uses custom_currency.json (CustomCurrencyConfig loads it, creates with defaults if missing)
            CustomCurrencyConfig.setConfigOverride(null);
            CustomCurrencyConfig.loadFromFile();
        } catch (Exception e) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to load Fabric config: {}", e.getMessage());
        }
    }

    /** Steps 1-3 → 250/500/750 CD. Other values used as raw CD. */
    public static int stepToCd(int raw) {
        return (raw >= 1 && raw <= 3) ? raw * 250 : raw;
    }

    private static String getDefaultConfigJson() {
        return """
                {
                  "cobbledollarsEmeraldRate": 3,
                  "syncCobbleDollarsBankRate": true,
                  "villagersAcceptCobbleDollars": true,
                  "useCobbleDollarsShopUi": true,
                  "useRctTradesOverhaul": true,
                  "useDatapackTrades": true,
                  "excludedVillagerProfessionNamespaces": ["casinorocket"],
                  "_comment_emeraldSteps": "1=250, 2=500, 3=750 CD per emerald (CobbleDollars scale)",
                  "_comment_excluded": "Villagers with professions from these mod namespaces use their native UI (e.g. CasinoRocket Casino Workers). Remove 'casinorocket' to use CobbleDollars shop for them.",
                  "_comment": "Edit config/cobbledollars_villagers_overhaul_rca/custom_currency.json for Relic Coins, Poketokens, etc."
                }
                """;
    }
}
