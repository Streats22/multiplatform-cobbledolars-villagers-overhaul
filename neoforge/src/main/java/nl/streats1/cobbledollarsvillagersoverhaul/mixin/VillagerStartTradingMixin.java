package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerStartTradingMixin {

    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void cobbledollars_villagers_overhaul_rca$mcaRedirectToShop(Player player, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (McaTradeRedirect.tryOpenCobbleDollarsShop(self, player)) {
            ci.cancel();
        }
    }
}
