package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaVillagerCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

/**
 * Client-side MCA trade fallback when the logical server redirect did not run (integrated client / timing).
 */
public final class FabricMcaShopRequests {

    private FabricMcaShopRequests() {
    }

    public static boolean tryRequestShopFromClient(AbstractVillager villager, Player player) {
        if (!player.level().isClientSide()) {
            return false;
        }
        if (!McaVillagerCompat.isModLoaded() || !McaVillagerCompat.isMcaVillager(villager)) {
            return false;
        }
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }
        if (villager instanceof Villager v) {
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            if (Config.isVillagerProfessionExcluded(profId)) {
                return false;
            }
        }
        FabricPendingCustomShopScreen.beginAwaitingShopData(villager.getId(), true);
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(villager.getId()));
        return true;
    }
}
