package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Read/write CobbleDollars bank.json for the bank editor.
 * Format: { "bank": [ { "item": "id", "price": X }, ... ] }
 */
public final class BankConfig {
    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";
    private static final String BANK_FILE = "bank.json";

    private BankConfig() {
    }

    private static Path getBankFile() {
        return CobbleDollarsConfigHelper.getConfigDirectory().resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(BANK_FILE);
    }

    public static List<BankEntryRecord> loadEntries() {
        Path file = getBankFile();
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        try {
            String content = Files.readString(file);
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
                int price = o.has("price") ? parsePrice(o.get("price")) : 0;
                if (price <= 0) continue;
                out.add(new BankEntryRecord(itemId, price));
            }
            return out;
        } catch (Exception ex) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to load bank config: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    private static int parsePrice(JsonElement el) {
        if (el == null || el.isJsonNull()) return 0;
        if (el.isJsonPrimitive()) {
            var p = el.getAsJsonPrimitive();
            if (p.isNumber()) return p.getAsInt();
            if (p.isString()) {
                String s = p.getAsString().trim().toLowerCase();
                int mult = 1;
                if (s.endsWith("k")) {
                    mult = 1000;
                    s = s.substring(0, s.length() - 1);
                } else if (s.endsWith("m")) {
                    mult = 1_000_000;
                    s = s.substring(0, s.length() - 1);
                }
                try {
                    return (int) (Double.parseDouble(s) * mult);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    public static void saveEntries(List<BankEntryRecord> entries) {
        Path file = getBankFile();
        try {
            Files.createDirectories(file.getParent());
            JsonArray bank = new JsonArray();
            for (BankEntryRecord e : entries) {
                JsonObject o = new JsonObject();
                o.addProperty("item", e.itemId());
                o.addProperty("price", e.price());
                bank.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("bank", bank);
            Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Saved bank config to {}", file);
        } catch (Exception ex) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to save bank config: {}", ex.getMessage());
        }
    }
}
