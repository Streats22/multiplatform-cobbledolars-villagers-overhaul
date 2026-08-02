package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.*;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which villager entities use the config shop instead of vanilla trades.
 * Persisted to villager_shops.json.
 */
public final class VillagerShopConfig {

    private static final String CONFIG_FILE = "villager_shops.json";
    private static final String KEY = "useConfigShop";
    private static Path configDirOverride;

    private static final Set<UUID> useConfigShop = ConcurrentHashMap.newKeySet();

    /**
     * When true, {@link #save()} refuses to write so a failed/corrupt load cannot
     * persist an empty (or stale) in-memory set over the on-disk assignments.
     */
    private static volatile boolean saveBlockedAfterFailedLoad;

    public static void setConfigRoot(Path root) {
        configDirOverride = root;
    }

    private static Path getConfigFile() {
        Path root = configDirOverride != null ? configDirOverride : Path.of("config");
        return root.resolve(CobbleDollarsVillagersOverhaulRca.MOD_ID).resolve(CONFIG_FILE);
    }

    /**
     * Visible for tests: parse assignment UUIDs from JSON without mutating live state.
     *
     * @throws JsonParseException if the document is not a usable object/array shape
     * @throws IllegalArgumentException not used; invalid UUID strings are skipped
     */
    static Set<UUID> parseAssignments(String content) {
        JsonElement root = JsonParser.parseString(content);
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException("villager_shops.json root must be a JSON object");
        }
        JsonElement arr = root.getAsJsonObject().get(KEY);
        if (arr == null || !arr.isJsonArray()) {
            throw new JsonParseException("villager_shops.json missing \"" + KEY + "\" array");
        }
        Set<UUID> parsed = new LinkedHashSet<>();
        for (JsonElement el : arr.getAsJsonArray()) {
            if (el.isJsonPrimitive()) {
                try {
                    parsed.add(UUID.fromString(el.getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return parsed;
    }

    /** Visible for tests. */
    static boolean isSaveBlockedAfterFailedLoad() {
        return saveBlockedAfterFailedLoad;
    }

    /** Visible for tests. */
    static void resetStateForTests() {
        useConfigShop.clear();
        saveBlockedAfterFailedLoad = false;
        configDirOverride = null;
    }

    public static void load() {
        saveBlockedAfterFailedLoad = false;
        Path file = getConfigFile();
        if (!Files.isRegularFile(file)) {
            useConfigShop.clear();
            return;
        }
        try {
            String content = Files.readString(file);
            Set<UUID> parsed = parseAssignments(content);
            // Replace only after a successful parse so corrupt JSON cannot wipe assignments.
            useConfigShop.clear();
            useConfigShop.addAll(parsed);
        } catch (Exception e) {
            saveBlockedAfterFailedLoad = true;
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn(
                    "Failed to load villager shop config (refusing save to protect on-disk data): {}",
                    e.getMessage());
        }
    }

    public static void save() {
        if (saveBlockedAfterFailedLoad) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error(
                    "Skipping villager shop config save because the last load failed; fix or restore {}",
                    getConfigFile());
            return;
        }
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            for (UUID u : useConfigShop) {
                arr.add(u.toString());
            }
            JsonObject obj = new JsonObject();
            obj.add(KEY, arr);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(obj);
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.writeString(tmp, json);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
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
