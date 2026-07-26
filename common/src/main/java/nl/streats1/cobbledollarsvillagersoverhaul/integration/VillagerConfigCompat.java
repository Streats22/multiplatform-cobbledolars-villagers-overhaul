package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;

/**
 * Optional integration with <a href="https://modrinth.com/mod/villagerconfig">VillagerConfig</a>.
 * VillagerConfig injects datapack trades from {@code updateTrades}; the shop only needs to generate
 * offers when the list is still empty.
 * <p>
 * Do <strong>not</strong> clear a non-empty offer list on shop open: that recreates
 * {@link net.minecraft.world.item.trading.MerchantOffer} instances and resets uses/demand, which
 * lets players infinitely restock limited trades by closing and reopening the shop.
 */
public final class VillagerConfigCompat {

    private static Boolean modLoaded;
    private static Method getTradeTable;

    private VillagerConfigCompat() {
    }

    public static boolean isLoaded() {
        if (modLoaded == null) {
            try {
                Class.forName("me.drex.villagerconfig.common.VillagerConfig");
                modLoaded = true;
            } catch (ClassNotFoundException e) {
                modLoaded = false;
            }
        }
        return modLoaded;
    }

    private static void resolveGetTradeTable() {
        if (getTradeTable != null) {
            return;
        }
        try {
            Class<?> cvd = Class.forName("me.drex.villagerconfig.common.util.CustomVillagerData");
            getTradeTable = cvd.getMethod("getTradeTable", Villager.class);
            getTradeTable.setAccessible(true);
        } catch (Throwable t) {
            getTradeTable = null;
        }
    }

    /**
     * Whether existing offers should be wiped before regenerating for a VillagerConfig custom table.
     * Always {@code false}: non-empty lists hold live stock/uses; empty lists need no clear.
     */
    static boolean shouldClearOffersBeforeRefresh(boolean hasCustomTradeTable, int existingOfferCount) {
        return false;
    }

    /**
     * Run before reading {@link Villager#getOffers()} for the shop on the server.
     */
    public static void prepareVillagerForShop(ServerLevel level, Villager villager) {
        if (villager == null || level == null) {
            return;
        }
        if (isLoaded()) {
            resolveGetTradeTable();
            if (getTradeTable != null) {
                try {
                    boolean hasCustomTradeTable = getTradeTable.invoke(null, villager) != null;
                    MerchantOffers offers = villager.getOffers();
                    int existingOfferCount = offers == null ? 0 : offers.size();
                    if (shouldClearOffersBeforeRefresh(hasCustomTradeTable, existingOfferCount) && offers != null) {
                        offers.clear();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        MerchantTradeGenerationHelper.ensureMerchantOffersReady(level, villager);
    }
}
