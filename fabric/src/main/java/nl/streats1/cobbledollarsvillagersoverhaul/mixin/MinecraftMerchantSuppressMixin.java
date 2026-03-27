package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import nl.streats1.cobbledollarsvillagersoverhaul.fabric.FabricPendingCustomShopScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMerchantSuppressMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cobbledollars_villagers_overhaul_rca$suppressMerchantWhileCustomShopPending(Screen screen, CallbackInfo ci) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return;
        }
        if (FabricPendingCustomShopScreen.shouldSuppressMerchantScreen(screen)) {
            ci.cancel();
        }
    }
}
