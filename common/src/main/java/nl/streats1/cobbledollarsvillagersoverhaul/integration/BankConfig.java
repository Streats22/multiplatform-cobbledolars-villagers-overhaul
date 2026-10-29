package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.util.JsonPriceParser;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BankConfig {
    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";
    private static final String BANK_FILE = "bank.json";

    private BankConfig() {
    }

    private static Path getBankFile() {
        return ModConfig.getConfigDirectory().resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(BANK_FILE);
    }

    public static List<BankEntryRecord> loadEntries() {
        Path file = getBankFile();
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        try {
            return parseEntries(Files.readString(file));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    /** Raw file text for syncing to ops; empty object when the file does not exist yet. */
    public static String readRaw() {
        try {
            Path file = getBankFile();
            return Files.isRegularFile(file) ? Files.readString(file) : "{}";
        } catch (Exception ex) {
            return "{}";
        }
    }

    public static void writeRaw(String json) throws java.io.IOException {
        Path file = getBankFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    public static List<BankEntryRecord> parseEntries(String content) {
        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            List<BankEntryRecord> out = new ArrayList<>();
            if (!root.has("bank")) return out;
            JsonElement bankEl = root.get("bank");
            if (!bankEl.isJsonArray()) return out;
            for (JsonElement e : bankEl.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String itemId = o.has("item") ? o.get("item").getAsString() : null;
                if (itemId == null || itemId.isEmpty()) continue;
                int price = o.has("price") ? JsonPriceParser.parse(o.get("price")) : 0;
                if (price <= 0) continue;
                out.add(new BankEntryRecord(itemId, price));
            }
            return out;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static void saveEntries(List<BankEntryRecord> entries) {
        try {
            writeRaw(toJson(entries));
        } catch (Exception ex) {
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
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
}
