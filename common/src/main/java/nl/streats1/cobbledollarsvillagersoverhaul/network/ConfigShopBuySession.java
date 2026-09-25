package nl.streats1.cobbledollarsvillagersoverhaul.network;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catalog chosen when ShopData was sent. Buy packets also carry {@code fromConfigShop}, but that
 * boolean is client-controlled and must not select the config catalog on its own.
 */
public final class ConfigShopBuySession {

    private static final ConcurrentHashMap<UUID, Set<UUID>> OPEN_CONFIG_SHOPS = new ConcurrentHashMap<>();

    private ConfigShopBuySession() {
    }

    public static void remember(UUID playerId, UUID merchantId, boolean openedAsConfigShop) {
        if (playerId == null || merchantId == null) {
            return;
        }
        if (!openedAsConfigShop) {
            forget(playerId, merchantId);
            return;
        }
        OPEN_CONFIG_SHOPS.compute(playerId, (ignored, open) -> {
            Set<UUID> merchants = open != null ? open : ConcurrentHashMap.newKeySet();
            merchants.add(merchantId);
            return merchants;
        });
    }

    public static void forget(UUID playerId, UUID merchantId) {
        if (playerId == null || merchantId == null) {
            return;
        }
        OPEN_CONFIG_SHOPS.computeIfPresent(playerId, (ignored, open) -> {
            open.remove(merchantId);
            return open.isEmpty() ? null : open;
        });
    }

    public static void forgetPlayer(UUID playerId) {
        if (playerId != null) {
            OPEN_CONFIG_SHOPS.remove(playerId);
        }
    }

    public static boolean wasOpenedAsConfigShop(UUID playerId, UUID merchantId) {
        if (playerId == null || merchantId == null) {
            return false;
        }
        Set<UUID> open = OPEN_CONFIG_SHOPS.get(playerId);
        return open != null && open.contains(merchantId);
    }

    static void clear() {
        OPEN_CONFIG_SHOPS.clear();
    }
}
