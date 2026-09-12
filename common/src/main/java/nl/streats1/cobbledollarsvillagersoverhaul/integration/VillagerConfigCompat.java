package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;

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

    public static void prepareVillagerForShop(ServerLevel level, Villager villager) {
        if (villager == null || level == null) {
            return;
        }
        if (isLoaded()) {
            resolveGetTradeTable();
            if (getTradeTable != null) {
                try {
                    if (getTradeTable.invoke(null, villager) != null) {
                        MerchantOffers offers = villager.getOffers();
                        if (offers != null) {
                            offers.clear();
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        MerchantTradeGenerationHelper.ensureMerchantOffersReady(level, villager);
    }
}
