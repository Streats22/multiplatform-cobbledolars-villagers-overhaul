package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The personal shop of one assigned villager: its own buy catalog (categories with items and prices), its own
 * sell list (items the villager buys from players) and its own tab names. Stored as
 * {@code villager_shops/<uuid>.json}; a villager without a file uses the shared default shop.
 */
public final class VillagerShopData {

    public static final String SECTION_SHOP = "shop";
    public static final String SECTION_BANK = "bank";
    public static final String SECTION_LABELS = "labels";

    private static final String SUBDIR = "villager_shops";

    public record Data(ShopLabels.Values labels,
                       Map<String, List<ShopEntryRecord>> shop,
                       List<BankEntryRecord> bank) {
    }

    private VillagerShopData() {
    }

    private static Path file(UUID villager) {
        return ModConfig.getConfigDirectory().resolve(CobbleDollarsVillagersOverhaulRca.MOD_ID)
                .resolve(SUBDIR).resolve(villager + ".json");
    }

    public static boolean exists(UUID villager) {
        return Files.isRegularFile(file(villager));
    }

    /** The villager's own shop, or {@code null} when it has none (it then uses the default shop). */
    public static Data load(UUID villager) {
        Path file = file(villager);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            ShopLabels.Values labels = ShopLabels.fromJson(root.has(SECTION_LABELS) ? root.get(SECTION_LABELS).toString() : "");
            Map<String, List<ShopEntryRecord>> shop = root.has(SECTION_SHOP)
                    ? DefaultShopConfig.parseCategories(root.get(SECTION_SHOP).toString())
                    : new LinkedHashMap<>();
            List<BankEntryRecord> bank = root.has(SECTION_BANK)
                    ? BankConfig.parseEntries(root.get(SECTION_BANK).toString())
                    : new ArrayList<>();
            return new Data(labels != null ? labels : new ShopLabels.Values("", "", "", ""), shop, bank);
        } catch (Exception ex) {
            return null;
        }
    }

    public static void save(UUID villager, Data data) {
        JsonObject root = new JsonObject();
        root.add(SECTION_LABELS, JsonParser.parseString(ShopLabels.toJson(data.labels())));
        root.add(SECTION_SHOP, JsonParser.parseString(DefaultShopConfig.toJson(data.shop())));
        root.add(SECTION_BANK, JsonParser.parseString(BankConfig.toJson(data.bank())));
        try {
            Path file = file(villager);
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    /** A copy of the current default shop, no sell list, and tab names left blank (= server names). */
    public static Data copyOfDefaults() {
        Map<String, List<ShopEntryRecord>> shop = new LinkedHashMap<>();
        for (Map.Entry<String, List<ShopEntryRecord>> e : DefaultShopConfig.loadCategories().entrySet()) {
            shop.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return new Data(new ShopLabels.Values("", "", "", ""), shop, new ArrayList<>());
    }

    /** Gives the villager its own shop the first time it is assigned; an existing shop is kept. */
    public static void ensureExists(UUID villager) {
        if (!exists(villager)) {
            save(villager, copyOfDefaults());
        }
    }

    public static Data loadOrDefaults(UUID villager) {
        Data data = load(villager);
        return data != null ? data : copyOfDefaults();
    }

    /** One section as JSON text for the editors; {@code null} for an unknown section. */
    public static String readSection(UUID villager, String section) {
        Data data = loadOrDefaults(villager);
        return switch (section) {
            case SECTION_SHOP -> DefaultShopConfig.toJson(data.shop());
            case SECTION_BANK -> BankConfig.toJson(data.bank());
            case SECTION_LABELS -> ShopLabels.toJson(data.labels());
            default -> null;
        };
    }

    public static void writeSection(UUID villager, String section, String json) {
        Data data = loadOrDefaults(villager);
        JsonElement check = JsonParser.parseString(json);
        if (!check.isJsonObject()) {
            throw new IllegalArgumentException(section);
        }
        switch (section) {
            case SECTION_SHOP -> data = new Data(data.labels(), DefaultShopConfig.parseCategories(json), data.bank());
            case SECTION_BANK -> data = new Data(data.labels(), data.shop(), BankConfig.parseEntries(json));
            case SECTION_LABELS -> {
                ShopLabels.Values labels = ShopLabels.fromJson(json);
                data = new Data(labels != null ? labels : data.labels(), data.shop(), data.bank());
            }
            default -> throw new IllegalArgumentException(section);
        }
        save(villager, data);
    }
}
