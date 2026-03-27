package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantMenuTraderAccessor {

    @Accessor("trader")
    Merchant cobbledollars_villagers_overhaul_rca$getTrader();
}
