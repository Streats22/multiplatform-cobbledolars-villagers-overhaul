package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.world.item.ItemStack;

import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.math.BigInteger;

import fr.harmex.cobbledollars.common.world.item.trading.shop.Bank;

@Mixin(Bank.class)
public class BankMixin {

    private static Constructor<?> offerCtor;

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void onContains(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && stack != null && !stack.isEmpty()) {
            if (CustomCurrencyConfig.isCurrencyItem(stack)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "get(Lnet/minecraft/world/item/ItemStack;)Lfr/harmex/cobbledollars/common/world/item/trading/shop/Offer;", at = @At("RETURN"), cancellable = true)
    private void onGet(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() == null && stack != null && !stack.isEmpty()) {
            int value = CustomCurrencyConfig.getCurrencyValue(stack);
            if (value > 0) {
                Object offer = createOffer(stack, value);
                if (offer != null) {
                    cir.setReturnValue(offer);
                }
            }
        }
    }

    private static Object createOffer(ItemStack stack, int cobbleDollarsPerItem) {
        try {
            if (offerCtor == null) {
                Class<?> offerClass = Class.forName("fr.harmex.cobbledollars.common.world.item.trading.shop.Offer");
                offerCtor = offerClass.getConstructor(ItemStack.class, BigInteger.class, int.class);
            }
            ItemStack single = stack.copy();
            single.setCount(1);
            return offerCtor.newInstance(single, BigInteger.valueOf(cobbleDollarsPerItem), 0);
        } catch (Throwable t) {
            return null;
        }
    }
}
