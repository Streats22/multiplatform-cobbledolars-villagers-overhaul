package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeCommandCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MCA Trade button → CobbleDollars shop. Uses reflection (no {@code @Shadow}) so generic
 * {@code entity} field / missing MCA still fail soft via {@link Pseudo}.
 */
@Pseudo
@Mixin(targets = "net.conczin.mca.entity.interaction.VillagerCommandHandler", remap = false)
public abstract class McaTradeCommandMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void cobbledollars_villagers_overhaul_rca$redirectTrade(
            ServerPlayer player, String command, CallbackInfoReturnable<Boolean> cir) {
        if (McaTradeCommandCompat.tryRedirectTrade(this, player, command)) {
            cir.setReturnValue(true);
        }
    }
}
