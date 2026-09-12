package nl.streats1.cobbledollarsvillagersoverhaul;

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
