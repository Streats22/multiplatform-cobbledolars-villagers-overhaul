package nl.streats1.cobbledollarsvillagersoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerCobbleDollarsHandler;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformConfig;
import org.slf4j.Logger;

public class CobbleDollarsVillagersOverhaulRca {
    public static final String MOD_ID = "cobbledollars_villagers_overhaul_rca";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CobbleDollarsVillagersOverhaulRca() {
        // Initialize platform-specific configuration
        Config.loadConfig();
        
        CobbleDollarsShopPayloadHandlers.registerPayloads();
        VillagerCobbleDollarsHandler.register();
        
        LOGGER.info("CobbleDollars Villagers Overhaul initialized on {}", 
            PlatformConfig.getPlatformInfo());
    }

    public boolean onEntityInteract(Entity target, boolean isClientSide, boolean isSneaking,
                                   Runnable cancelAction, java.util.function.IntSupplier getId) {
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isModLoaded()) return false;

        // RCT trainers: Let them work completely normally
        // RCT has its own shop system with series support from datapacks
        if (RctTrainerAssociationCompat.isTrainerAssociation(target)) {
            // Don't interfere at all - let RCT handle everything
            // But add debugging to see if offers are being generated
            if (isClientSide) {
                LOGGER.info("RCT Integration: Client-side RCT trainer detected, entity: {}", target.getClass().getSimpleName());
            } else {
                LOGGER.info("RCT Integration: Server-side RCT trainer detected, checking offers...");
                if (target instanceof net.minecraft.world.item.trading.Merchant merchant) {
                    var offers = merchant.getOffers();
                    LOGGER.info("RCT Integration: Trainer has {} offers", offers.size());
                    if (offers.isEmpty()) {
                        LOGGER.warn("RCT Integration: Trainer has no offers! This might be a configuration issue.");
                        // Try to trigger offer generation using reflection
                        try {
                            Class<?> trainerClass = Class.forName("rctmod.entity.TrainerAssociation");
                            if (trainerClass.isInstance(target)) {
                                // Try multiple methods to generate offers
                                String[] methods = {"generateOffers", "updateTrades", "refreshTrades", "initializeTrades"};
                                for (String method : methods) {
                                    try {
                                        java.lang.reflect.Method m = trainerClass.getMethod(method, ServerPlayer.class);
                                        LOGGER.info("RCT Integration: Trying to call {} on trainer", method);
                                        // We need a ServerPlayer, but we don't have one here
                                        // This is just for debugging - actual offer generation should happen naturally
                                    } catch (NoSuchMethodException e) {
                                        // Method doesn't exist
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("RCT Integration: Failed to debug trainer offers", e);
                        }
                    }
                }
            }
            return false;
        }

        if (target instanceof Villager villager) {
            VillagerProfession prof = villager.getVillagerData().getProfession();
            if (prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) return false;
        } else if (!(target instanceof WanderingTrader)) {
            return false;
        }

        cancelAction.run();
        return true;
    }
}
