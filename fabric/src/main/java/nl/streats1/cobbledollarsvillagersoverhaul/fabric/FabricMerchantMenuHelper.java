package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;

final class FabricMerchantMenuHelper {

    private FabricMerchantMenuHelper() {
    }

    static Merchant getTrader(AbstractContainerMenu menu) {
        if (!(menu instanceof MerchantMenu merchantMenu)) {
            return null;
        }
        for (Class<?> c = merchantMenu.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                if (Merchant.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        return (Merchant) f.get(merchantMenu);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
