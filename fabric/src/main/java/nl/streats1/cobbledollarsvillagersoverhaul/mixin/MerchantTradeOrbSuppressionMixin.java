package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.ShopTradeOrbSuppression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public class MerchantTradeOrbSuppressionMixin {

    @Inject(method = "shouldRewardExp", at = @At("HEAD"), cancellable = true)
    private void cobbledollars_villagers_overhaul_rca$skipOrbSpawn(CallbackInfoReturnable<Boolean> cir) {
        if (ShopTradeOrbSuppression.isSuppressingTradeOrbs()) {
            cir.setReturnValue(false);
        }
    }
}
