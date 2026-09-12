package nl.streats1.cobbledollarsvillagersoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaVillagerCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.MerchantInteractCompat;
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
                                   ItemStack heldItem, Runnable cancelAction) {
        if (!CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (typeId != null && "cobbledollars".equals(typeId.getNamespace())) {
            return false;
        }

        // Optional-mod / vanilla item / sneak passthrough (issue #51: lasso, backpacks, Carry On).
        if (MerchantInteractCompat.shouldDeferShopOverride(target, isSneaking, heldItem)) {
            LOGGER.debug("[shop] deferring shop override (sneak/item/entity exclusion), entity={} sneak={}",
                    target.getType().getDescriptionId(), isSneaking);
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

        // When MCA compat is on, let MCA handle normal right-click for all MCA entities.
        // CobbleDollars shop opens from Trade/shift-trade via startTrading redirect mixin.
        if (McaVillagerCompat.shouldDeferNormalRightClick(target)) {
            return false;
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
