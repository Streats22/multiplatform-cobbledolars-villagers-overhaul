package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.Merchant;

/**
 * MCA / vanilla merchant menus use a {@link MenuProvider} wrapper around the {@link Merchant}.
 */
public final class McaMenuProviderHelper {

    private McaMenuProviderHelper() {
    }

    public static AbstractVillager extractTradeVillager(MenuProvider menuProvider) {
        Merchant merchant = extractMerchant(menuProvider);
        if (merchant instanceof AbstractVillager villager) {
            return villager;
        }
        return null;
    }

    public static Merchant extractMerchant(MenuProvider menuProvider) {
        if (menuProvider instanceof Merchant merchant) {
            return merchant;
        }
        if (menuProvider instanceof AbstractVillager villager) {
            return villager;
        }
        Class<?> providerClass = menuProvider.getClass();
        if (providerClass.isRecord()) {
            for (var component : providerClass.getRecordComponents()) {
                Class<?> type = component.getType();
                if (Merchant.class.isAssignableFrom(type)) {
                    try {
                        Object result = component.getAccessor().invoke(menuProvider);
                        if (result instanceof Merchant merchant) {
                            return merchant;
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }
        for (String methodName : new String[]{"merchant", "getMerchant", "getTrader", "trader", "getVillager"}) {
            try {
                var method = providerClass.getMethod(methodName);
                Object result = method.invoke(menuProvider);
                if (result instanceof Merchant merchant) {
                    return merchant;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        for (Class<?> c = providerClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (var field : c.getDeclaredFields()) {
                if (Merchant.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object result = field.get(menuProvider);
                        if (result instanceof Merchant merchant) {
                            return merchant;
                        }
                    } catch (IllegalAccessException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
