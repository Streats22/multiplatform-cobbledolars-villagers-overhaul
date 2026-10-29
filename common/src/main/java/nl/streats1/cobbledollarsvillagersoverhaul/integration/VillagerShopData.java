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
    public static final String SECTION_TRADES = "trades";

    private static final String SUBDIR = "villager_shops";

    /** {@code sellTab} / {@code tradesTab}: whether the shop shows those tabs; both are off by default. */
    public record Data(ShopLabels.Values labels,
                       Map<String, List<ShopEntryRecord>> shop,
                       List<BankEntryRecord> bank,
                       boolean sellTab,
                       boolean tradesTab,
                       List<TradeRecord> trades) {
        public Data(ShopLabels.Values labels, Map<String, List<ShopEntryRecord>> shop, List<BankEntryRecord> bank) {
            this(labels, shop, bank, false, false, new ArrayList<>());
        }

        public Data(ShopLabels.Values labels, Map<String, List<ShopEntryRecord>> shop, List<BankEntryRecord> bank,
                    boolean sellTab, boolean tradesTab) {
            this(labels, shop, bank, sellTab, tradesTab, new ArrayList<>());
        }
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
            JsonObject tabs = root.has("tabs") && root.get("tabs").isJsonObject() ? root.getAsJsonObject("tabs") : new JsonObject();
            return new Data(labels != null ? labels : new ShopLabels.Values("", "", "", ""), shop, bank,
                    flag(tabs, "sell"), flag(tabs, "trades"),
                    root.has(SECTION_TRADES) ? parseTrades(root.get(SECTION_TRADES)) : new ArrayList<>());
        } catch (Exception ex) {
            return null;
        }
    }

    public static void save(UUID villager, Data data) {
        JsonObject root = new JsonObject();
        root.add(SECTION_LABELS, JsonParser.parseString(ShopLabels.toJson(data.labels())));
        root.add(SECTION_SHOP, JsonParser.parseString(DefaultShopConfig.toJson(data.shop())));
        root.add(SECTION_BANK, JsonParser.parseString(BankConfig.toJson(data.bank())));
        JsonObject tabs = new JsonObject();
        tabs.addProperty("sell", data.sellTab());
        tabs.addProperty("trades", data.tradesTab());
        root.add("tabs", tabs);
        root.add(SECTION_TRADES, tradesToJson(data.trades()));
        try {
            Path file = file(villager);
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    /** Accepts a list of trades, or an object holding one under {@code "trades"}. */
    public static List<TradeRecord> parseTrades(JsonElement el) {
        List<TradeRecord> out = new ArrayList<>();
        try {
            JsonElement list = el.isJsonObject() ? el.getAsJsonObject().get(SECTION_TRADES) : el;
            if (list == null || !list.isJsonArray()) return out;
            for (JsonElement e : list.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String give = text(o, "give");
                String get = text(o, "get");
                if (give.isEmpty() || get.isEmpty()) continue;
                out.add(new TradeRecord(give, number(o, "giveCount"), text(o, "give2"), number(o, "give2Count"), get, number(o, "getCount"),
                        o.has("dollars") ? Math.max(0, number(o, "dollars")) : 0));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static com.google.gson.JsonArray tradesToJson(List<TradeRecord> trades) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (TradeRecord t : trades) {
            JsonObject o = new JsonObject();
            o.addProperty("give", t.giveItem());
            o.addProperty("giveCount", t.giveCount());
            o.addProperty("give2", t.give2Item());
            o.addProperty("give2Count", t.give2Count());
            o.addProperty("get", t.getItem());
            o.addProperty("getCount", t.getCount());
            o.addProperty("dollars", t.dollars());
            arr.add(o);
        }
        return arr;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static int number(JsonObject o, String key) {
        try {
            return o.has(key) ? o.get(key).getAsInt() : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static boolean flag(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsBoolean();
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
            case SECTION_SHOP -> {
                JsonObject o = JsonParser.parseString(DefaultShopConfig.toJson(data.shop())).getAsJsonObject();
                JsonObject tabs = new JsonObject();
                tabs.addProperty("sell", data.sellTab());
                tabs.addProperty("trades", data.tradesTab());
                o.add("tabs", tabs);
                yield o.toString();
            }
            case SECTION_BANK -> BankConfig.toJson(data.bank());
            case SECTION_TRADES -> {
                JsonObject o = new JsonObject();
                o.add(SECTION_TRADES, tradesToJson(data.trades()));
                yield o.toString();
            }
            case SECTION_LABELS -> {
                JsonObject o = JsonParser.parseString(ShopLabels.toJson(data.labels())).getAsJsonObject();
                o.addProperty("showSell", data.sellTab());
                o.addProperty("showTrades", data.tradesTab());
                yield o.toString();
            }
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
            case SECTION_SHOP -> {
                boolean sell = data.sellTab();
                boolean trades = data.tradesTab();
                JsonObject tabs = check.getAsJsonObject().has("tabs") && check.getAsJsonObject().get("tabs").isJsonObject()
                        ? check.getAsJsonObject().getAsJsonObject("tabs") : null;
                if (tabs != null) {
                    sell = flag(tabs, "sell");
                    trades = flag(tabs, "trades");
                }
                data = new Data(data.labels(), DefaultShopConfig.parseCategories(json), data.bank(), sell, trades, data.trades());
            }
            case SECTION_BANK -> data = new Data(data.labels(), data.shop(), BankConfig.parseEntries(json), data.sellTab(), data.tradesTab(), data.trades());
            case SECTION_TRADES -> data = new Data(data.labels(), data.shop(), data.bank(), data.sellTab(), data.tradesTab(), parseTrades(check));
            case SECTION_LABELS -> {
                JsonObject o = check.getAsJsonObject();
                ShopLabels.Values current = data.labels();
                ShopLabels.Values labels = new ShopLabels.Values(
                        o.has("shop") ? text(o, "shop") : current.shop(),
                        o.has("buy") ? text(o, "buy") : current.buy(),
                        o.has("sell") ? text(o, "sell") : current.sell(),
                        o.has("trades") ? text(o, "trades") : current.trades());
                boolean sell = o.has("showSell") ? flag(o, "showSell") : data.sellTab();
                boolean trades = o.has("showTrades") ? flag(o, "showTrades") : data.tradesTab();
                data = new Data(labels, data.shop(), data.bank(), sell, trades, data.trades());
            }
            default -> throw new IllegalArgumentException(section);
        }
        save(villager, data);
    }
}
