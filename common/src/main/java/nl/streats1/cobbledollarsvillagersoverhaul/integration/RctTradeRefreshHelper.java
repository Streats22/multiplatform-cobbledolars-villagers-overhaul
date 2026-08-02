package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Safely refreshes Radical Cobblemon Trainers trainer-association item offers.
 * <p>
 * Older code reflectively invoked every {@code method_*} declared on the entity until
 * {@code itemOffers} grew. On intermediary runtimes that can execute arbitrary no-arg
 * Minecraft overrides (not just {@code updateTrades}), mutating or clearing trainer state.
 */
public final class RctTradeRefreshHelper {

    private RctTradeRefreshHelper() {
    }

    /** Visible for tests: only void instance methods with zero parameters are candidates. */
    static boolean isSafeUpdateTradesCandidate(Method method) {
        if (method == null) {
            return false;
        }
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (method.getParameterCount() != 0) {
            return false;
        }
        if (!void.class.equals(method.getReturnType())) {
            return false;
        }
        String name = method.getName();
        return "updateTrades".equals(name) || name.startsWith("method_");
    }

    /**
     * Prefer an exact {@code updateTrades} name; otherwise keep intermediary {@code method_*}
     * candidates. Never accepts methods with parameters.
     */
    static List<Method> selectUpdateTradesCandidates(Method[] declaredMethods) {
        List<Method> named = new ArrayList<>();
        List<Method> intermediary = new ArrayList<>();
        if (declaredMethods == null) {
            return named;
        }
        for (Method method : declaredMethods) {
            if (!isSafeUpdateTradesCandidate(method)) {
                continue;
            }
            if ("updateTrades".equals(method.getName())) {
                named.add(method);
            } else {
                intermediary.add(method);
            }
        }
        return named.isEmpty() ? intermediary : named;
    }

    private static MerchantOffers copyOffers(Object offers) {
        if (!(offers instanceof MerchantOffers merchantOffers)) {
            return null;
        }
        MerchantOffers copy = new MerchantOffers();
        copy.addAll(merchantOffers);
        return copy;
    }

    private static boolean offersGrew(Object before, Object after) {
        if (before == null && after instanceof MerchantOffers afterOffers) {
            return !afterOffers.isEmpty();
        }
        if (before instanceof MerchantOffers beforeOffers && after instanceof MerchantOffers afterOffers) {
            return afterOffers.size() > beforeOffers.size();
        }
        return false;
    }

    /**
     * Invokes a safe {@code updateTrades} candidate when {@code itemOffers} is present, restoring
     * the previous offer list when a candidate does not grow offers.
     */
    public static void refreshItemOffers(Entity entity) {
        if (entity == null) {
            return;
        }
        Field itemOffersField;
        try {
            itemOffersField = entity.getClass().getDeclaredField("itemOffers");
            itemOffersField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            return;
        }

        List<Method> candidates = selectUpdateTradesCandidates(entity.getClass().getDeclaredMethods());
        for (Method method : candidates) {
            try {
                Object before = itemOffersField.get(entity);
                MerchantOffers snapshot = copyOffers(before);
                method.setAccessible(true);
                method.invoke(entity);
                Object after = itemOffersField.get(entity);
                if (offersGrew(before, after)) {
                    return;
                }
                if (snapshot != null || before == null) {
                    itemOffersField.set(entity, snapshot);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static void updateOffersForPlayer(Entity entity, Player player) {
        if (entity == null || player == null) {
            return;
        }
        try {
            Method updateOffersFor = entity.getClass().getMethod("updateOffersFor", Player.class);
            updateOffersFor.setAccessible(true);
            updateOffersFor.invoke(entity, player);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
