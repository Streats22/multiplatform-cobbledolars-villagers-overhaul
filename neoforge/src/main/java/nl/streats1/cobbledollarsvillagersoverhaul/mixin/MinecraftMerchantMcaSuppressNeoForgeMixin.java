package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import nl.streats1.cobbledollarsvillagersoverhaul.neoforge.NeoForgeMerchantMcaRedirect;
import nl.streats1.cobbledollarsvillagersoverhaul.neoforge.NeoForgePendingCustomShopMerchantSuppress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMerchantMcaSuppressNeoForgeMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cobbledollars_villagers_overhaul_rca$neoForgeMerchantShim(Screen screen, CallbackInfo ci) {
        if (NeoForgeMerchantMcaRedirect.suppressIncomingMerchantScreen(screen)) {
            ci.cancel();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return;
        }
        if (NeoForgePendingCustomShopMerchantSuppress.shouldSuppressMerchantScreen(screen)) {
            ci.cancel();
        }
    }
}
