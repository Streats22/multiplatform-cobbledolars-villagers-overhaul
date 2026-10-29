package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.util.JsonPriceParser;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BankConfig {
    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";
    private static final String LEGACY_FILE = "bank.json";
    private static final String CURRENT_FILE = "default_bank.json";

    private BankConfig() {
    }

    private static Path file(String name) {
        return ModConfig.getConfigDirectory().resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(name);
    }

    private static Path activeFile() {
        Path current = file(CURRENT_FILE);
        if (Files.isRegularFile(current)) {
            return current;
        }
        Path legacy = file(LEGACY_FILE);
        return Files.isRegularFile(legacy) ? legacy : null;
    }

    public static List<BankEntryRecord> loadEntries() {
        Path active = activeFile();
        if (active == null) return new ArrayList<>();
        try {
            return parseEntries(Files.readString(active));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    /** Raw file text for syncing to ops; empty object when no bank file exists yet. */
    public static String readRaw() {
        try {
            Path active = activeFile();
            return active != null ? Files.readString(active) : "{}";
        } catch (Exception ex) {
            return "{}";
        }
    }

    public static void writeRaw(String json) throws java.io.IOException {
        List<BankEntryRecord> entries = parseEntries(json);
        Path current = file(CURRENT_FILE);
        Path legacy = file(LEGACY_FILE);
        boolean hasCurrent = Files.isRegularFile(current);
        boolean hasLegacy = Files.isRegularFile(legacy);
        if (hasCurrent || !hasLegacy) {
            write(current, currentFormat(entries));
        }
        if (hasLegacy || !hasCurrent) {
            write(legacy, toJson(entries));
        }
        CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
    }

    /** Accepts both the legacy {"bank":[...]} object and the CobbleDollars 6.1 list. */
    public static List<BankEntryRecord> parseEntries(String content) {
        List<BankEntryRecord> out = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(content);
            JsonElement list = root.isJsonObject() ? root.getAsJsonObject().get("bank") : root;
            if (list == null || !list.isJsonArray()) {
                return out;
            }
            for (JsonElement e : list.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String itemId = o.has("item") ? o.get("item").getAsString() : null;
                if (itemId == null || itemId.isEmpty()) continue;
                int price = o.has("price") ? JsonPriceParser.parse(o.get("price")) : 0;
                if (price <= 0) continue;
                out.add(new BankEntryRecord(itemId, price));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void saveEntries(List<BankEntryRecord> entries) {
        try {
            writeRaw(toJson(entries));
        } catch (Exception ignored) {
        }
    }

    public static String toJson(List<BankEntryRecord> entries) {
        JsonArray bank = new JsonArray();
        for (BankEntryRecord e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("item", e.itemId());
            o.addProperty("price", e.price());
            bank.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("bank", bank);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private static String currentFormat(List<BankEntryRecord> entries) {
        JsonArray bank = new JsonArray();
        for (BankEntryRecord e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("item", e.itemId());
            o.addProperty("price", String.valueOf(e.price()));
            bank.add(o);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(bank);
    }

    private static void write(Path path, String content) throws java.io.IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
