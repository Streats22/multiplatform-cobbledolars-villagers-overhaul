package nl.streats1.cobbledollarsvillagersoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerCobbleDollarsHandler;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import org.slf4j.Logger;

public class CobbleDollarsVillagersOverhaulRca {
    public static final String MOD_ID = "cobbledollars_villagers_overhaul_rca";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CobbleDollarsVillagersOverhaulRca() {
        CobbleDollarsShopPayloadHandlers.registerPayloads();
        VillagerCobbleDollarsHandler.register();
    }

    public boolean onEntityInteract(Entity target, boolean isClientSide, boolean isSneaking,
                                   Runnable cancelAction, java.util.function.IntSupplier getId) {
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isModLoaded()) return false;

        if (RctTrainerAssociationCompat.isTrainerAssociation(target)) {
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
