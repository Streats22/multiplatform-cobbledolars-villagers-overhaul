package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerShopConfig;

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
        CobbleDollarsConfigHelper.setConfigRoot(configDir);
        VillagerShopConfig.setConfigRoot(configDir);
        VillagerShopConfig.load();
        Path dir = configDir.resolve(CONFIG_SUBDIR);
        Path file = dir.resolve(CONFIG_FILE);

        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(dir);
                String defaultJson = getDefaultConfigJson();
                Files.writeString(file, defaultJson);
            }

            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("cobbledollarsEmeraldRate")) {
                Config.setCobbledollarsEmeraldRate(
                        nl.streats1.cobbledollarsvillagersoverhaul.integration.EmeraldRateHelper.normalizeCdPerEmerald(
                                root.get("cobbledollarsEmeraldRate").getAsInt()));
            }
            CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
            if (root.has("syncCobbleDollarsBankRate")) {
                Config.setSyncCobbleDollarsBankRate(root.get("syncCobbleDollarsBankRate").getAsBoolean());
            }
            if (root.has("villagersAcceptCobbleDollars")) {
                Config.setVillagersAcceptCobbleDollars(root.get("villagersAcceptCobbleDollars").getAsBoolean());
            }
            if (root.has("freeMinimumEmeraldTrade")) {
                Config.setFreeMinimumEmeraldTrade(root.get("freeMinimumEmeraldTrade").getAsBoolean());
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
            if (root.has("excludedVillagerProfessionIds") && root.get("excludedVillagerProfessionIds").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("excludedVillagerProfessionIds");
                java.util.List<String> list = new java.util.ArrayList<>();
                for (JsonElement el : arr) {
                    if (el.isJsonPrimitive()) list.add(el.getAsString());
                }
                Config.setExcludedVillagerProfessionIds(list);
            }

            CustomCurrencyConfig.setConfigOverride(null);
            CustomCurrencyConfig.loadFromFile();
            ItemPriceConfig.loadAndApply();
        } catch (Exception e) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to load Fabric config: {}", e.getMessage());
        }
    }

    private static String getDefaultConfigJson() {
        return nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.fabricMainConfigJson();
    }
}
