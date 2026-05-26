package nl.streats1.cobbledollarsvillagersoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaVillagerCompat;
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

    public boolean onEntityInteract(Entity target, boolean isClientSide, boolean isSneaking,
                                   Runnable cancelAction) {
        if (!CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (typeId != null && "cobbledollars".equals(typeId.getNamespace())) {
            return false;
        }

        if (RctTrainerAssociationCompat.isTrainerAssociation(target)) {
            if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
                return false;
            }
            if (!isClientSide && !Config.USE_RCT_TRADES_OVERHAUL) {
                return false;
            }
            cancelAction.run();
            return true;
        }

        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
            return false;
        }

        // Handle MCA villagers - they replace vanilla villagers
        if (McaVillagerCompat.isMcaVillager(target)) {
            cancelAction.run();
            return true;
        }

        if (target instanceof Villager villager) {
            VillagerProfession prof = villager.getVillagerData().getProfession();
            if (prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) return false;
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
            if (Config.isVillagerProfessionExcluded(profId)) return false;
        } else if (!(target instanceof WanderingTrader)) {
            return false;
        }

        cancelAction.run();
        return true;
    }
}
