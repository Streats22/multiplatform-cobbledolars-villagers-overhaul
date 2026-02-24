package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Trade cycling support - refreshes villager trades without breaking the workstation.
 * Compatible with Trade Cycling mod and Easy Villagers: relaxed requirements so cycling
 * works with mods that handle villagers differently (e.g. villagers in blocks).
 */
public final class TradeCyclingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Method setOffersMethod;

    static {
        try {
            // AbstractVillager.setOffers(MerchantOffers) is protected - use reflection
            setOffersMethod = Villager.class.getSuperclass().getDeclaredMethod("setOffers", MerchantOffers.class);
            setOffersMethod.setAccessible(true);
        } catch (Exception e) {
            LOGGER.warn("Could not resolve AbstractVillager.setOffers for trade cycling: {}", e.getMessage());
        }
    }

    /**
     * Check if a villager can have its trades cycled (refreshed).
     * Relaxed for Easy Villagers: only requires at least one offer.
     * Vanilla prefers: has workstation and not yet traded, but we allow cycling even otherwise.
     */
    public static boolean canCycleTrades(Villager villager) {
        if (villager == null) return false;
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
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE
                || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            LOGGER.debug("Cannot cycle: villager has no profession");
            return false;
        }
        try {
            // Method 1: Direct offers clear + repopulate (works for vanilla and many mods)
            if (setOffersMethod != null) {
                setOffers(villager, null);
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
                int villagerId = villager.getId();
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                // Schedule restore next tick so villager "loses" job then regains it; send shop data after
                serverLevel.getServer().execute(() -> {
                    villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, pos);
                    MerchantOffers after = villager.getOffers();
                    if (after != null && !after.isEmpty()) {
                        villager.setTradingPlayer(player);
                        LOGGER.info("Successfully cycled trades for villager at {} (workstation method)", villager.blockPosition());
                        if (onSuccess != null) onSuccess.run();
                    } else if (onSuccess != null) {
                        CobbleDollarsShopPayloadHandlers.handleRequestShopData(player, villagerId);
                    }
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
