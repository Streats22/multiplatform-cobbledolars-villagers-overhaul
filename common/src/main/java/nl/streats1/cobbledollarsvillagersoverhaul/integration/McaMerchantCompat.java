package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;

/**
 * MCA villagers may lazily generate trades. Only call {@code updateTrades}/{@code restock}
 * when the offer list is empty — vanilla {@code updateTrades} <em>appends</em> level listings
 * and will duplicate offers if invoked on every shop open.
 */
public final class McaMerchantCompat {

    private McaMerchantCompat() {
    }

    public static boolean shouldGenerateMissingTrades(boolean offersMissingOrEmpty) {
        return offersMissingOrEmpty;
    }

    public static void prepareForShop(ServerLevel level, Villager villager) {
        if (level == null || villager == null || !McaVillagerCompat.isMcaVillager(villager)) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        if (!shouldGenerateMissingTrades(offers == null || offers.isEmpty())) {
            MerchantOfferDedupe.removeIdenticalDuplicates(offers);
            return;
        }
        refreshTrades(level, villager);
        offers = villager.getOffers();
        if (shouldGenerateMissingTrades(offers == null || offers.isEmpty())) {
            invokeRestock(villager);
            refreshTrades(level, villager);
            offers = villager.getOffers();
        }
        MerchantOfferDedupe.removeIdenticalDuplicates(offers);
    }

    private static void refreshTrades(ServerLevel level, Villager villager) {
        Method withLevel = findUpdateTradesWithLevel(villager);
        try {
            if (withLevel != null) {
                withLevel.invoke(villager, level);
                return;
            }
            Method noArgs = findUpdateTradesNoArgs(villager);
            if (noArgs != null) {
                noArgs.invoke(villager);
            }
        } catch (Exception ignored) {
        }
    }

    private static void invokeRestock(Villager villager) {
        for (Class<?> c = villager.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method restock = c.getDeclaredMethod("restock");
                restock.setAccessible(true);
                restock.invoke(villager);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                return;
            }
        }
    }

    private static Method findUpdateTradesWithLevel(Villager villager) {
        for (Class<?> c = villager.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if ("updateTrades".equals(m.getName())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == ServerLevel.class) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    private static Method findUpdateTradesNoArgs(Villager villager) {
        for (Class<?> c = villager.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if ("updateTrades".equals(m.getName()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
