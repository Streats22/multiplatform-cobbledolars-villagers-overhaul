package nl.streats1.cobbledollarsvillagersoverhaul.client;

import net.minecraft.client.Minecraft;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.AssignedVillagersScreen;

import java.util.List;

/** Latest list of config-shop villagers sent by the server to an op. */
public final class ClientAssignedVillagers {
    private static List<String> uuids = List.of();
    private static List<String> labels = List.of();

    private ClientAssignedVillagers() {
    }

    public static void set(List<String> newUuids, List<String> newLabels) {
        uuids = List.copyOf(newUuids);
        labels = List.copyOf(newLabels);
        if (Minecraft.getInstance().screen instanceof AssignedVillagersScreen screen) {
            screen.onListChanged();
        }
    }

    public static List<String> uuids() {
        return uuids;
    }

    public static List<String> labels() {
        return labels;
    }
}
