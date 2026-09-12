package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Ensures MCA Reborn villagers have merchant offers populated before the CobbleDollars shop reads them.
 * <p>
 * MCA 7.7+ may lazy-generate trades; vanilla {@code startTrading} is cancelled by our mixin, so we must
 * refresh trades explicitly (not only when the offer list is empty).
 */
public final class McaMerchantCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private McaMerchantCompat() {
    }

    /**
     * Refresh MCA villager trades before building shop offer lists.
     */
    public static void prepareForShop(ServerLevel level, Villager villager) {
        if (level == null || villager == null || !McaVillagerCompat.isMcaVillager(villager)) {
            return;
        }
        refreshTrades(level, villager);
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            invokeRestock(villager);
            refreshTrades(level, villager);
            offers = villager.getOffers();
            if (offers == null || offers.isEmpty()) {
                LOGGER.debug("[mca] villager {} ({}) still has no offers after refresh (profession={})",
                        villager.getUUID(), villager.getType().getDescriptionId(),
                        villager.getVillagerData().getProfession());
            }
        }
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
        } catch (Exception e) {
            LOGGER.debug("[mca] updateTrades failed for {}: {}", villager.getUUID(), e.toString());
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
                LOGGER.debug("[mca] restock failed for {}: {}", villager.getUUID(), e.toString());
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
