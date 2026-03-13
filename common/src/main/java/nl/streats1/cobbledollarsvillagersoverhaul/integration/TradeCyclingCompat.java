package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Trade cycling support - refreshes villager trades without breaking the workstation.
 * Uses vanilla Villager/AbstractVillager APIs only. Trade Cycling and Easy Villagers mods
 * are optional: we have our own implementation. When those mods are present they can
 * coexist; when absent we work standalone.
 */
public final class TradeCyclingCompat {
    private static Method setOffersMethod;
    private static Method updateTradesMethod;

    static {
        Class<?> abstractVillager = Villager.class.getSuperclass();
        Class<?> villagerClass = Villager.class;
        for (String name : new String[]{"overrideOffers", "setOffers"}) {
            try {
                setOffersMethod = abstractVillager.getDeclaredMethod(name, MerchantOffers.class);
                setOffersMethod.setAccessible(true);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (setOffersMethod == null) {
            for (java.lang.reflect.Method m : abstractVillager.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == MerchantOffers.class) {
                    setOffersMethod = m;
                    setOffersMethod.setAccessible(true);
                    break;
                }
            }
        }
        for (String name : new String[]{"updateTrades", "fillRecipes"}) {
            try {
                updateTradesMethod = villagerClass.getDeclaredMethod(name);
                updateTradesMethod.setAccessible(true);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (updateTradesMethod == null) {
            for (java.lang.reflect.Method m : villagerClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && void.class.equals(m.getReturnType())) {
                    String mn = m.getName().toLowerCase();
                    if (mn.contains("trade") || mn.contains("recipe") || mn.contains("offer")) {
                        updateTradesMethod = m;
                        updateTradesMethod.setAccessible(true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check if a villager can have its trades cycled (refreshed).
     * Only level 1 villagers (novice, not yet traded) can cycle; once leveled up, trades are locked.
     */
    public static boolean canCycleTrades(Villager villager) {
        if (villager == null) return false;
        if (villager.getVillagerData().getLevel() > 1) return false;
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE
                || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            return false;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) return false;
        return true;
    }

    /**
     * Cycle (refresh) a villager's trades. Clears offers and regenerates from profession pool.
     * Uses setOffers(null) + getOffers() first; if that yields empty, tries workstation clear/restore.
     * @param onSuccess callback to run after cycle completes (e.g. send refreshed shop data). Called once.
     */
    public static boolean cycleTrades(Villager villager, ServerPlayer player, Runnable onSuccess) {
        if (villager == null) return false;
        if (villager.getVillagerData().getLevel() > 1) {
            return false;
        }
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE
                || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            return false;
        }
        try {
            if (setOffersMethod != null) {
                MerchantOffers empty = new MerchantOffers();
                setOffers(villager, empty);
                MerchantOffers current = villager.getOffers();
                if (current != null) current.clear();
                if (updateTradesMethod != null) {
                    try {
                        updateTradesMethod.invoke(villager);
                    } catch (Exception e) {
                    }
                } else {
                    setOffers(villager, null);
                }
                MerchantOffers newOffers = villager.getOffers();
                if (newOffers != null && !newOffers.isEmpty()) {
                    villager.setTradingPlayer(player);
                    if (onSuccess != null) onSuccess.run();
                    return true;
                }
            }

            Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (jobSite.isPresent() && villager.level() instanceof ServerLevel serverLevel) {
                GlobalPos pos = jobSite.get();
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                serverLevel.getServer().execute(() -> {
                    villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, pos);
                    serverLevel.getServer().execute(() -> {
                        villager.setTradingPlayer(player);
                        if (onSuccess != null) onSuccess.run();
                    });
                });
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setOffers(Villager villager, MerchantOffers offers) {
        if (setOffersMethod == null) return;
        try {
            setOffersMethod.invoke(villager, offers);
        } catch (Exception e) {
        }
    }
}
