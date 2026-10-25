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

    public static List<BankEntryRecord> loadEntries() {
        Path current = file(CURRENT_FILE);
        if (Files.isRegularFile(current)) {
            return parse(current);
        }
        Path legacy = file(LEGACY_FILE);
        if (Files.isRegularFile(legacy)) {
            return parse(legacy);
        }
        return new ArrayList<>();
    }

    public static void saveEntries(List<BankEntryRecord> entries) {
        boolean hasCurrent = Files.isRegularFile(file(CURRENT_FILE));
        boolean hasLegacy = Files.isRegularFile(file(LEGACY_FILE));
        try {
            if (hasCurrent || !hasLegacy) {
                write(file(CURRENT_FILE), currentFormat(entries));
            }
            if (hasLegacy || !hasCurrent) {
                write(file(LEGACY_FILE), legacyFormat(entries));
            }
        } catch (Exception ignored) {
        }
        CobbleDollarsConfigHelper.invalidateBankEmeraldPriceCache();
    }

    private static List<BankEntryRecord> parse(Path path) {
        List<BankEntryRecord> out = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(Files.readString(path));
            JsonElement list = root;
            if (root.isJsonObject()) {
                list = root.getAsJsonObject().get("bank");
            }
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

    private static JsonElement currentFormat(List<BankEntryRecord> entries) {
        JsonArray bank = new JsonArray();
        for (BankEntryRecord e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("item", e.itemId());
            o.addProperty("price", String.valueOf(e.price()));
            bank.add(o);
        }
        return bank;
    }

    private static JsonElement legacyFormat(List<BankEntryRecord> entries) {
        JsonArray bank = new JsonArray();
        for (BankEntryRecord e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("item", e.itemId());
            o.addProperty("price", e.price());
            bank.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("bank", bank);
        return root;
    }

    private static void write(Path path, JsonElement json) throws java.io.IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }
}
