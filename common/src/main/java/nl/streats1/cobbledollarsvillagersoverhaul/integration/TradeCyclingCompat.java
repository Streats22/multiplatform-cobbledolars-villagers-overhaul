package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.mojang.logging.LogUtils;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Trade cycling support - refreshes villager trades without breaking the workstation.
 * Uses vanilla Villager/AbstractVillager APIs only. Trade Cycling and Easy Villagers mods
 * are optional: we have our own implementation. When those mods are present they can
 * coexist; when absent we work standalone.
 */
public final class TradeCyclingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Method setOffersMethod;
    private static Method updateTradesMethod;

    static {
        Class<?> abstractVillager = Villager.class.getSuperclass();
        Class<?> villagerClass = Villager.class;
        for (String name : new String[]{"overrideOffers", "setOffers"}) {
            try {
                setOffersMethod = abstractVillager.getDeclaredMethod(name, MerchantOffers.class);
                setOffersMethod.setAccessible(true);
                LOGGER.debug("Resolved trade cycling method: {}.{}", abstractVillager.getSimpleName(), name);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (setOffersMethod == null) {
            for (java.lang.reflect.Method m : abstractVillager.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == MerchantOffers.class) {
                    setOffersMethod = m;
                    setOffersMethod.setAccessible(true);
                    LOGGER.debug("Resolved trade cycling method by signature: {}.{}", abstractVillager.getSimpleName(), m.getName());
                    break;
                }
            }
        }
        if (setOffersMethod == null) {
            LOGGER.warn("Could not resolve AbstractVillager method (MerchantOffers) for trade cycling - workstation fallback only");
        }
        // Villager.updateTrades() - populates offers from profession pool; needed for fresh random rolls
        for (String name : new String[]{"updateTrades", "fillRecipes"}) {
            try {
                updateTradesMethod = villagerClass.getDeclaredMethod(name);
                updateTradesMethod.setAccessible(true);
                LOGGER.debug("Resolved Villager.{} for trade regeneration", name);
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
                        LOGGER.debug("Resolved Villager.{} for trade regeneration (by name)", m.getName());
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
            LOGGER.debug("Cannot cycle: villager has leveled up (level {})", villager.getVillagerData().getLevel());
            return false;
        }
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE
                || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            LOGGER.debug("Cannot cycle: villager has no profession");
            return false;
        }
        try {
            // Method 1: Clear + regenerate. updateTrades() adds to existing list, so we must clear first.
            if (setOffersMethod != null) {
                MerchantOffers empty = new MerchantOffers();
                setOffers(villager, empty);
                MerchantOffers current = villager.getOffers();
                if (current != null) current.clear();
                if (updateTradesMethod != null) {
                    try {
                        updateTradesMethod.invoke(villager);
                    } catch (Exception e) {
                        LOGGER.debug("updateTrades reflection failed: {}", e.getMessage());
                    }
                } else {
                    setOffers(villager, null);
                }
                MerchantOffers newOffers = villager.getOffers();
                if (newOffers != null && !newOffers.isEmpty()) {
                    villager.setTradingPlayer(player);
                    LOGGER.info("Successfully cycled trades for villager at {} (direct method)", villager.blockPosition());
                    if (onSuccess != null) onSuccess.run();
                    return true;
                }
            }

            // Method 2: Workstation clear/restore (Easy Villagers-style, works when villager has JOB_SITE)
            Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (jobSite.isPresent() && villager.level() instanceof ServerLevel serverLevel) {
                GlobalPos pos = jobSite.get();
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                // Restore JOB_SITE next tick; villager may need an extra tick to regenerate offers
                serverLevel.getServer().execute(() -> {
                    villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, pos);
                    serverLevel.getServer().execute(() -> {
                        MerchantOffers after = villager.getOffers();
                        villager.setTradingPlayer(player);
                        if (after != null && !after.isEmpty()) {
                            LOGGER.info("Successfully cycled trades for villager at {} (workstation method)", villager.blockPosition());
                        }
                        if (onSuccess != null) onSuccess.run();
                    });
                });
                return true;
            }

            LOGGER.warn("Could not cycle trades for villager at {}: no workstation and direct method failed", villager.blockPosition());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to cycle villager trades: {}", e.getMessage(), e);
            return false;
        }
    }

    private static void setOffers(Villager villager, MerchantOffers offers) {
        if (setOffersMethod == null) return;
        try {
            setOffersMethod.invoke(villager, offers);
        } catch (Exception e) {
            LOGGER.warn("Reflection setOffers failed: {}", e.getMessage());
        }
    }
}
