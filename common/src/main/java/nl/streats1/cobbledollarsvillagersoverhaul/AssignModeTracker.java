package nl.streats1.cobbledollarsvillagersoverhaul;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side: players in assign/unassign mode (shift+left-click villager).
 */
public final class AssignModeTracker {
    public enum Mode {ASSIGN, UNASSIGN}

    private static final Map<UUID, Mode> mode = new ConcurrentHashMap<>();

    public static void setMode(UUID playerUuid, Mode m) {
        if (m != null) mode.put(playerUuid, m);
        else mode.remove(playerUuid);
    }

    public static boolean isInAssignMode(UUID playerUuid) {
        return mode.get(playerUuid) == Mode.ASSIGN;
    }

    public static boolean isInUnassignMode(UUID playerUuid) {
        return mode.get(playerUuid) == Mode.UNASSIGN;
    }

    public static boolean isInAnyMode(UUID playerUuid) {
        return mode.containsKey(playerUuid);
    }

    public static void clear(UUID playerUuid) {
        mode.remove(playerUuid);
    }
}
