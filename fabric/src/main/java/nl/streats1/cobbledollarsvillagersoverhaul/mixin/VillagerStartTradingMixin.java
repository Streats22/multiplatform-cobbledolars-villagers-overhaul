package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public class VillagerStartTradingMixin {

    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void cobbledollars_villagers_overhaul_rca$mcaRedirectToShop(Player player, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (McaIntegration.tryOpenShop(self, player)) {
            ci.cancel();
        }
    }
}
