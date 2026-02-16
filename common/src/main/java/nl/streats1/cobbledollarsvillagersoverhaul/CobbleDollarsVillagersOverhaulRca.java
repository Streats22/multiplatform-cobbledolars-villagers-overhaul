package nl.streats1.cobbledollarsvillagersoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerCobbleDollarsHandler;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import org.slf4j.Logger;

public class CobbleDollarsVillagersOverhaulRca {
    public static final String MOD_ID = "cobbledollars_villagers_overhaul_rca";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CobbleDollarsVillagersOverhaulRca() {
        CobbleDollarsShopPayloadHandlers.registerPayloads();
        VillagerCobbleDollarsHandler.register();
    }

    /**
     * When true, we should send RequestShopData from the client (CobbleDollars loaded so we can show our UI).
     * When false, we still cancel vanilla so the server never opens the merchant screen, but we won't get shop data.
     */
    public boolean shouldSendShopRequest(boolean isClientSide) {
        return isClientSide && Config.USE_COBBLEDOLLARS_SHOP_UI && CobbleDollarsIntegration.isModLoaded();
    }

    /**
     * Returns true when we want to take over the interaction (cancel vanilla merchant screen).
     * We cancel whenever our shop UI config is on and the entity is a valid merchant, so the server
     * never opens the vanilla trade screen. RequestShopData is sent only when {@link #shouldSendShopRequest} is true.
     */
    public boolean onEntityInteract(Entity target, boolean isClientSide, boolean isSneaking,
                                   Runnable cancelAction, java.util.function.IntSupplier getId) {
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) return false;

        if (RctTrainerAssociationCompat.isTrainerAssociation(target)) {
            if (!Config.USE_RCT_TRADES_OVERHAUL) return false;
            cancelAction.run();
            return true;
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
