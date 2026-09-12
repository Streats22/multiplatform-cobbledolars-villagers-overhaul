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
import java.util.ArrayList;
import java.util.List;

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
            if (root.has("enableMcaCompatibility")) {
                Config.setEnableMcaCompatibility(root.get("enableMcaCompatibility").getAsBoolean());
            }
            if (root.has("skipShopOverrideWhenSneaking")) {
                Config.setSkipShopOverrideWhenSneaking(root.get("skipShopOverrideWhenSneaking").getAsBoolean());
            }
            List<String> excludedNamespaces = readStringArray(root, "excludedVillagerProfessionNamespaces");
            if (excludedNamespaces != null) {
                Config.setExcludedVillagerProfessionNamespaces(excludedNamespaces);
            }
            List<String> excludedProfessionIds = readStringArray(root, "excludedVillagerProfessionIds");
            if (excludedProfessionIds != null) {
                Config.setExcludedVillagerProfessionIds(excludedProfessionIds);
            }
            List<String> excludedEntityTypeNamespaces = readStringArray(root, "excludedEntityTypeNamespaces");
            if (excludedEntityTypeNamespaces != null) {
                Config.setExcludedEntityTypeNamespaces(excludedEntityTypeNamespaces);
            }
            List<String> excludedEntityTypeIds = readStringArray(root, "excludedEntityTypeIds");
            if (excludedEntityTypeIds != null) {
                Config.setExcludedEntityTypeIds(excludedEntityTypeIds);
            }
            List<String> passthroughItemIds = readStringArray(root, "passthroughInteractItemIds");
            if (passthroughItemIds != null) {
                Config.setPassthroughInteractItemIds(passthroughItemIds);
            }
            List<String> passthroughItemNamespaces = readStringArray(root, "passthroughInteractItemNamespaces");
            if (passthroughItemNamespaces != null) {
                Config.setPassthroughInteractItemNamespaces(passthroughItemNamespaces);
            }

            CustomCurrencyConfig.setConfigOverride(null);
            CustomCurrencyConfig.loadFromFile();
            ItemPriceConfig.loadAndApply();
        } catch (Exception e) {
        }
    }

    private static String getDefaultConfigJson() {
        return nl.streats1.cobbledollarsvillagersoverhaul.integration.ModConfigDefaults.fabricMainConfigJson();
    }

        private static List<String> readStringArray(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return null;
        }
        JsonArray arr = root.getAsJsonArray(key);
        List<String> list = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonPrimitive()) {
                list.add(el.getAsString());
            }
        }
        return list;
    }
}
