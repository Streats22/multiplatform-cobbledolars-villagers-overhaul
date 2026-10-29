package nl.streats1.cobbledollarsvillagersoverhaul.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.BankConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DefaultShopConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.nio.charset.StandardCharsets;

/**
 * Lets operators read and change the shop, bank, item price and currency config on the server. The editors
 * run on the client, so on a dedicated server they would otherwise only edit the admin's own files.
 */
public final class AdminConfigService {

    public static final String SHOP = "shop";
    public static final String BANK = "bank";
    public static final String ITEM_PRICES = "item_prices";
    public static final String CURRENCY = "currency";

    private static final int MAX_BYTES = 256 * 1024;
    private static final int OP_LEVEL = 2;

    private AdminConfigService() {
    }

    public static void handleRequest(ServerPlayer player, String kind) {
        if (!player.hasPermissions(OP_LEVEL)) {
            return;
        }
        String json = read(kind);
        if (json != null) {
            PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.AdminConfigData(kind, json));
        }
    }

    public static void handleSave(ServerPlayer player, String kind, String json) {
        if (!player.hasPermissions(OP_LEVEL)) {
            return;
        }
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES || !isValid(kind, json)) {
            return;
        }
        try {
            write(kind, json);
        } catch (Exception ex) {
            return;
        }
        handleRequest(player, kind);
    }

    static String read(String kind) {
        return switch (kind) {
            case SHOP -> DefaultShopConfig.readRaw();
            case BANK -> BankConfig.readRaw();
            case ITEM_PRICES -> ItemPriceConfig.readRaw();
            case CURRENCY -> CustomCurrencyConfig.readRaw();
            default -> null;
        };
    }

    static boolean isValid(String kind, String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            return switch (kind) {
                case SHOP, BANK, ITEM_PRICES -> root.isJsonObject();
                case CURRENCY -> root.isJsonArray();
                default -> false;
            };
        } catch (Exception ex) {
            return false;
        }
    }

    private static void write(String kind, String json) throws java.io.IOException {
        switch (kind) {
            case SHOP -> DefaultShopConfig.writeRaw(json);
            case BANK -> BankConfig.writeRaw(json);
            case ITEM_PRICES -> {
                ItemPriceConfig.writeRaw(json);
                ItemPriceConfig.apply(ItemPriceConfig.parseEntries(json));
            }
            case CURRENCY -> {
                var entries = CustomCurrencyConfig.parseEntries(json);
                CustomCurrencyConfig.replaceEntries(entries);
                CustomCurrencyConfig.writeEntriesToFile(entries);
            }
            default -> throw new IllegalArgumentException(kind);
        }
    }
}
