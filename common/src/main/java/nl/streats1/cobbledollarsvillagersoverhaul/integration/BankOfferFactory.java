package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds CobbleDollars bank {@code Offer} instances reflectively. The constructor shape differs between
 * CobbleDollars versions, so any public constructor made of an {@link ItemStack}, one price number and
 * optional extra ints is accepted instead of a single hard-coded signature.
 */
public final class BankOfferFactory {

    private static final Map<String, Constructor<?>> CTORS = new HashMap<>();

    private BankOfferFactory() {
    }

    public static Object create(String offerClassName, ItemStack stack, int cobbleDollarsPerItem) {
        try {
            Constructor<?> ctor = CTORS.get(offerClassName);
            if (ctor == null) {
                ctor = findConstructor(Class.forName(offerClassName));
                if (ctor == null) return null;
                CTORS.put(offerClassName, ctor);
            }
            ItemStack single = stack.copy();
            single.setCount(1);
            Class<?>[] types = ctor.getParameterTypes();
            Object[] args = new Object[types.length];
            boolean priceSet = false;
            for (int i = 0; i < types.length; i++) {
                if (types[i] == ItemStack.class) {
                    args[i] = single;
                } else if (!priceSet) {
                    args[i] = price(types[i], cobbleDollarsPerItem);
                    priceSet = true;
                } else {
                    args[i] = zero(types[i]);
                }
            }
            return ctor.newInstance(args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Constructor<?> findConstructor(Class<?> offerClass) {
        Constructor<?> best = null;
        for (Constructor<?> c : offerClass.getConstructors()) {
            Class<?>[] types = c.getParameterTypes();
            int stacks = 0;
            int numbers = 0;
            boolean ok = true;
            for (Class<?> t : types) {
                if (t == ItemStack.class) stacks++;
                else if (isNumeric(t)) numbers++;
                else ok = false;
            }
            if (!ok || stacks != 1 || numbers < 1) continue;
            if (best == null || prefer(c, best)) best = c;
        }
        return best;
    }

    private static boolean prefer(Constructor<?> candidate, Constructor<?> current) {
        return candidate.getParameterCount() > current.getParameterCount();
    }

    private static boolean isNumeric(Class<?> t) {
        return t == BigInteger.class || t == BigDecimal.class || t == int.class || t == Integer.class
                || t == long.class || t == Long.class;
    }

    private static Object price(Class<?> t, int value) {
        if (t == BigInteger.class) return BigInteger.valueOf(value);
        if (t == BigDecimal.class) return BigDecimal.valueOf(value);
        if (t == long.class || t == Long.class) return (long) value;
        return value;
    }

    private static Object zero(Class<?> t) {
        if (t == BigInteger.class) return BigInteger.ZERO;
        if (t == BigDecimal.class) return BigDecimal.ZERO;
        if (t == long.class || t == Long.class) return 0L;
        return 0;
    }
}
