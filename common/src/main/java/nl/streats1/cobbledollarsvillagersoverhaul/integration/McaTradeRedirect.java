package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;

/**
 * Redirects MCA {@link AbstractVillager#startTrading} into the CobbleDollars shop pipeline.
 */
public final class McaTradeRedirect {

    private McaTradeRedirect() {
    }

    public static boolean tryOpenCobbleDollarsShop(AbstractVillager villager, Player player) {
        if (!McaVillagerCompat.isModLoaded() || !McaVillagerCompat.isMcaVillager(villager)) {
            return false;
        }
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isAvailable()) {
            return false;
        }
        if (!McaVillagerCompat.canTradeWithProfession(villager)) {
            return false;
        }
        if (villager instanceof Villager v) {
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            if (Config.isVillagerProfessionExcluded(profId)) {
                return false;
            }
        }
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        CobbleDollarsShopPayloadHandlers.handleRequestShopData(sp, villager.getId());
        return true;
    }
}
