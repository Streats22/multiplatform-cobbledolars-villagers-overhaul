package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;

/**
 * Redirects MCA trade ({@code startTrading} / {@code openTradingScreen}) into the CobbleDollars shop pipeline.
 */
public final class McaTradeRedirect {

    private McaTradeRedirect() {
    }

    public static boolean tryOpenCobbleDollarsShop(AbstractVillager villager, Player player) {
        if (!McaVillagerCompat.isModLoaded() || !McaVillagerCompat.isMcaVillager(villager)) {
            return false;
        }
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }
        if (!passesProfessionGate(villager)) {
            return false;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        MerchantTradeGenerationHelper.ensureMerchantOffersReady(sp.serverLevel(), villager);
        CobbleDollarsShopPayloadHandlers.handleRequestShopData(sp, villager.getId());
        return true;
    }

    private static boolean passesProfessionGate(AbstractVillager villager) {
        if (!(villager instanceof Villager v)) {
            return true;
        }
        VillagerProfession prof = v.getVillagerData().getProfession();
        if (prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) {
            return false;
        }
        ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
        return !Config.isVillagerProfessionExcluded(profId);
    }
}
