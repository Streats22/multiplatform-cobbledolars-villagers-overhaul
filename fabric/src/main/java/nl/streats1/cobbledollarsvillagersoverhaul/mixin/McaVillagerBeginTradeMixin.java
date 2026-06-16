package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric: MCA trade runs through {@code VillagerEntityMCA} private {@code copiedBeginTradeWith} and
 * {@code startTrading}, not vanilla {@code Villager.startTrading} / {@code Merchant.openTradingScreen} mixins.
 */
@Mixin(targets = "net.conczin.mca.entity.VillagerEntityMCA", remap = false)
public class McaVillagerBeginTradeMixin {

    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobbledollars_villagers_overhaul_rca$redirectMcaStartTrading(Player player, CallbackInfo ci) {
        if (tryOpenShop((AbstractVillager) (Object) this, player)) {
            ci.cancel();
        }
    }

    @Inject(method = "copiedBeginTradeWith", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobbledollars_villagers_overhaul_rca$redirectMcaCopiedBeginTrade(Player player, CallbackInfo ci) {
        if (tryOpenShop((AbstractVillager) (Object) this, player)) {
            ci.cancel();
        }
    }

    private static boolean tryOpenShop(AbstractVillager villager, Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        return McaTradeRedirect.tryOpenCobbleDollarsShop(villager, sp);
    }
}
