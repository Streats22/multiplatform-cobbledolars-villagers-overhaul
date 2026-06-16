package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import nl.streats1.cobbledollarsvillagersoverhaul.fabric.FabricMcaShopRequests;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backup hook on the {@link Merchant} default method (works on NeoForge; may apply on some Fabric setups).
 * Primary Fabric MCA path is {@link McaVillagerBeginTradeMixin} + {@link PlayerOpenMenuMcaRedirectMixin}.
 */
@Mixin(Merchant.class)
public interface MerchantOpenTradingMixin {

    @Inject(
            method = "openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbledollars_villagers_overhaul_rca$redirectOpenTradingScreen(
            Player player, Component displayName, int level, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof AbstractVillager villager)) {
            return;
        }
        if (McaTradeRedirect.tryOpenCobbleDollarsShop(villager, player)) {
            ci.cancel();
            return;
        }
        if (player.level().isClientSide() && FabricMcaShopRequests.tryRequestShopFromClient(villager, player)) {
            ci.cancel();
        }
    }
}
