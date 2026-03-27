package nl.streats1.cobbledollarsvillagersoverhaul;

/**
 * While set, {@code AbstractVillager.rewardTradeXp} is coerced to skip spawning experience orbs.
 * Our shop grants the same trade XP directly via {@link net.minecraft.server.level.ServerPlayer#giveExperiencePoints(int)}.
 */
public final class ShopTradeOrbSuppression {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ShopTradeOrbSuppression() {
    }

    public static void enter() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int d = DEPTH.get();
        if (d > 0) {
            DEPTH.set(d - 1);
        }
    }

    public static boolean isSuppressingTradeOrbs() {
        return DEPTH.get() > 0;
    }
}
