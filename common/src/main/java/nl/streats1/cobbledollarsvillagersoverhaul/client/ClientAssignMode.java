package nl.streats1.cobbledollarsvillagersoverhaul.client;

/**
 * Client-side: whether player is in assign/unassign mode (shift+left-click villager).
 */
public final class ClientAssignMode {
    private ClientAssignMode() {
    }

    private static boolean inMode;

    public static boolean isInMode() {
        return inMode;
    }

    public static void setInMode(boolean on) {
        inMode = on;
    }
}
