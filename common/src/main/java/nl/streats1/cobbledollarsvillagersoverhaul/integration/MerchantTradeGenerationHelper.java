package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;

/**
 * Ensures {@link AbstractVillager} merchant offers are generated before the CobbleDollars shop reads them.
 * <p>
 * <a href="https://modrinth.com/mod/villagerconfig">VillagerConfig</a> injects datapack trades from
 * {@code updateTrades(ServerLevel)}; if that has not run yet, {@link AbstractVillager#getOffers()} can be empty
 * when the shop opens without the vanilla merchant menu flow.
 */
public final class MerchantTradeGenerationHelper {

    private MerchantTradeGenerationHelper() {
    }

    private static Method findUpdateTradesWithLevel(AbstractVillager merchant) {
        for (Class<?> c = merchant.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!"updateTrades".equals(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == ServerLevel.class) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    private static Method findUpdateTradesNoArgs(AbstractVillager merchant) {
        for (Class<?> c = merchant.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if ("updateTrades".equals(m.getName()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * If the merchant has no offers yet, invokes {@code updateTrades} so mods like VillagerConfig can populate trades.
     */
    public static void ensureMerchantOffersReady(ServerLevel level, AbstractVillager merchant) {
        if (merchant == null || level == null) {
            return;
        }
        MerchantOffers offers = merchant.getOffers();
        if (offers != null && !offers.isEmpty()) {
            return;
        }
        Method withLevel = findUpdateTradesWithLevel(merchant);
        try {
            if (withLevel != null) {
                withLevel.invoke(merchant, level);
                return;
            }
            Method noArgs = findUpdateTradesNoArgs(merchant);
            if (noArgs != null) {
                noArgs.invoke(merchant);
            }
        } catch (Exception ignored) {
        }
    }
}
