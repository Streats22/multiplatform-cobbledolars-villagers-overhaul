package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.fabric.FabricMcaShopRequests;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaMenuProviderHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeRedirect;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaVillagerCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * MCA {@code openTradingScreen} opens {@code player.openMenu(MerchantMenuProvider)} — not the villager entity.
 */
@Mixin(Player.class)
public class PlayerOpenMenuMcaRedirectMixin {

    @Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbledollars_villagers_overhaul_rca$redirectMcaTradeMenu(
            MenuProvider menuProvider, CallbackInfoReturnable<OptionalInt> cir) {
        tryRedirectMcaTradeMenu(menuProvider, cir);
    }

    private void tryRedirectMcaTradeMenu(MenuProvider menuProvider, CallbackInfoReturnable<OptionalInt> cir) {
        AbstractVillager villager = McaMenuProviderHelper.extractTradeVillager(menuProvider);
        if (villager == null || !McaVillagerCompat.isMcaVillager(villager)) {
            return;
        }
        Player player = (Player) (Object) this;
        Player tradingPlayer = villager.getTradingPlayer();
        if (tradingPlayer != null && tradingPlayer != player) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            if (McaTradeRedirect.tryOpenCobbleDollarsShop(villager, sp)) {
                cir.setReturnValue(OptionalInt.empty());
                cir.cancel();
            }
            return;
        }
        if (player.level().isClientSide() && FabricMcaShopRequests.tryRequestShopFromClient(villager, player)) {
            cir.setReturnValue(OptionalInt.empty());
            cir.cancel();
        }
    }
}
