package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

/**
 * Tracks which villager entities use the config shop instead of vanilla trades.
 * Persisted to villager_shops.json.
 */
public final class VillagerShopConfig {

    private static final String CONFIG_FILE = "villager_shops.json";
    private static final String KEY = "useConfigShop";
    private static Path configDirOverride;

    private static final Set<UUID> useConfigShop = ConcurrentHashMap.newKeySet();

    public static void setConfigRoot(Path root) {
        configDirOverride = root;
    }

    private static Path getConfigFile() {
        Path root = configDirOverride != null ? configDirOverride : Path.of("config");
        return root.resolve(CobbleDollarsVillagersOverhaulRca.MOD_ID).resolve(CONFIG_FILE);
    }

    public static void load() {
        useConfigShop.clear();
        Path file = getConfigFile();
        if (!Files.isRegularFile(file)) return;
        try {
            String content = Files.readString(file);
            JsonElement root = JsonParser.parseString(content);
            if (root == null || !root.isJsonObject()) return;
            JsonElement arr = root.getAsJsonObject().get(KEY);
            if (arr == null || !arr.isJsonArray()) return;
            for (JsonElement el : arr.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    try {
                        useConfigShop.add(UUID.fromString(el.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to load villager shop config: {}", e.getMessage());
        }
    }

    public static void save() {
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            for (UUID u : useConfigShop) {
                arr.add(u.toString());
            }
            JsonObject obj = new JsonObject();
            obj.add(KEY, arr);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(obj));
        } catch (Exception e) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to save villager shop config: {}", e.getMessage());
        }
    }

    public static boolean usesConfigShop(UUID villagerUuid) {
        return useConfigShop.contains(villagerUuid);
    }

    public static void add(UUID villagerUuid) {
        useConfigShop.add(villagerUuid);
        save();
    }

    public static void remove(UUID villagerUuid) {
        useConfigShop.remove(villagerUuid);
        save();
    }

    public static List<UUID> getAll() {
        return new ArrayList<>(useConfigShop);
    }
}
