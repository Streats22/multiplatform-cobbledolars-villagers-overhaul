package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.ShopTradeOrbSuppression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Villager.class, WanderingTrader.class})
public class MerchantTradeOrbSuppressionMixin {

    @Redirect(
            method = "rewardTradeXp",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/trading/MerchantOffer;shouldRewardExp()Z"
            )
    )
    private boolean cobbledollars_villagers_overhaul_rca$skipOrbSpawn(MerchantOffer offer) {
        if (ShopTradeOrbSuppression.isSuppressingTradeOrbs()) {
            return false;
        }
        return offer.shouldRewardExp();
    }
}
