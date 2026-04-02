package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.math.BigInteger;

import fr.harmex.cobbledollars.common.world.item.trading.shop.Bank;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;

/**
 * Injects our custom currency items into CobbleDollars' bank so they can be deposited.
 * When CobbleDollars checks contains(ItemStack) or get(ItemStack), we add support for
 * items from our CustomCurrencyConfig (custom_currency.json / Edit currencies).
 */
@Mixin(Bank.class)
public class BankMixin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Constructor<?> offerCtor;

    /**
     * Name-only: full descriptors fail AP validation when stub uses Loom namedElements (ItemStack → intermediary in bytecode).
     */
    @Inject(method = "contains", at = @At("RETURN"), cancellable = true, remap = false)
    private void onContains(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && stack != null && !stack.isEmpty()) {
            if (CustomCurrencyConfig.isCurrencyItem(stack)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "get", at = @At("RETURN"), cancellable = true, remap = false)
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
            LOGGER.warn("Failed to create CobbleDollars Offer for custom currency: {}", t.getMessage());
            return null;
        }
    }
}
