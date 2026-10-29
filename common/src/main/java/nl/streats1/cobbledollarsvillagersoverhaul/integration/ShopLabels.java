package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Optional shop title and tab labels, stored server-side in {@code shop_labels.json} and synced to clients
 * with the server shop config. A blank value falls back to the language-file default, so resource-pack
 * renames still work.
 */
public final class ShopLabels {
    public static final int MAX_LENGTH = 32;

    private static final String CONFIG_FILE = "shop_labels.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";
    private static final String LANG_PREFIX = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Data data;
    private static volatile Values override;

    private ShopLabels() {
    }

    public record Values(String shop, String buy, String sell, String trades) {
    }

    private static final class Data {
        String shop = "";
        String buy = "";
        String sell = "";
        String trades = "";
    }

    private static Path file() {
        return ModConfig.getConfigDirectory().resolve(CONFIG_SUBDIR).resolve(CONFIG_FILE);
    }

    private static Data ensureLoaded() {
        if (data != null) return data;
        Data loaded = null;
        try {
            Path file = file();
            if (Files.isRegularFile(file)) {
                loaded = GSON.fromJson(Files.readString(file), Data.class);
            }
        } catch (Exception ignored) {
        }
        data = loaded != null ? loaded : new Data();
        return data;
    }

    public static void reload() {
        data = null;
    }

    /** Client side: labels of the villager whose shop is open; blank fields fall back to the server labels. */
    public static void setOverride(Values values) {
        override = values;
    }

    public static Values values() {
        Data d = ensureLoaded();
        return new Values(nonNull(d.shop), nonNull(d.buy), nonNull(d.sell), nonNull(d.trades));
    }

    /** Client side: adopt the labels the server sent, in memory only. */
    public static void applySynced(Values values) {
        Data d = new Data();
        d.shop = clean(values.shop());
        d.buy = clean(values.buy());
        d.sell = clean(values.sell());
        d.trades = clean(values.trades());
        data = d;
    }

    public static void save(Values values) {
        Data d = new Data();
        d.shop = clean(values.shop());
        d.buy = clean(values.buy());
        d.sell = clean(values.sell());
        d.trades = clean(values.trades());
        data = d;
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(d));
        } catch (Exception ignored) {
        }
    }

    public static String shopTitle() {
        return resolve(pick(ensureLoaded().shop, override != null ? override.shop() : null), "shop");
    }

    public static String buyTab() {
        return resolve(pick(ensureLoaded().buy, override != null ? override.buy() : null), "buy");
    }

    public static String sellTab() {
        return resolve(pick(ensureLoaded().sell, override != null ? override.sell() : null), "sell");
    }

    public static String tradesTab() {
        return resolve(pick(ensureLoaded().trades, override != null ? override.trades() : null), "trades");
    }

    /** Parses {"shop":..,"buy":..,"sell":..,"trades":..}; null or blank input means no override. */
    public static Values fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            var o = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            return new Values(text(o, "shop"), text(o, "buy"), text(o, "sell"), text(o, "trades"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Values v) {
        var o = new com.google.gson.JsonObject();
        o.addProperty("shop", clean(v.shop()));
        o.addProperty("buy", clean(v.buy()));
        o.addProperty("sell", clean(v.sell()));
        o.addProperty("trades", clean(v.trades()));
        return o.toString();
    }

    private static String text(com.google.gson.JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? clean(o.get(key).getAsString()) : "";
    }

    private static String pick(String global, String villager) {
        return clean(villager).isEmpty() ? global : villager;
    }

    private static String resolve(String custom, String langKey) {
        String c = clean(custom);
        return c.isEmpty() ? Component.translatable(LANG_PREFIX + langKey).getString() : c;
    }

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > MAX_LENGTH ? t.substring(0, MAX_LENGTH) : t;
    }

    private static String nonNull(String s) {
        return s != null ? s : "";
    }
}
