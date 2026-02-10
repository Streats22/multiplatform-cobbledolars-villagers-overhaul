package nl.streats1.cobbledollarsvillagersoverhaul.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class PlatformNetwork {
    @FunctionalInterface
    public interface ClientToServerSender {
        void send(CustomPacketPayload payload);
    }

    @FunctionalInterface
    public interface ServerToClientSender {
        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    private static volatile ClientToServerSender clientToServerSender;
    private static volatile ServerToClientSender serverToClientSender;

    private PlatformNetwork() {
    }

    public static void setClientToServerSender(ClientToServerSender sender) {
        clientToServerSender = Objects.requireNonNull(sender);
    }

    public static void setServerToClientSender(ServerToClientSender sender) {
        serverToClientSender = Objects.requireNonNull(sender);
    }

    public static boolean canSendToServer() {
        return clientToServerSender != null;
    }

    public static boolean canSendToPlayer() {
        return serverToClientSender != null;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientToServerSender sender = clientToServerSender;
        if (sender != null) {
            sender.send(payload);
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerToClientSender sender = serverToClientSender;
        if (sender != null) {
            sender.send(player, payload);
        }
    }
}
