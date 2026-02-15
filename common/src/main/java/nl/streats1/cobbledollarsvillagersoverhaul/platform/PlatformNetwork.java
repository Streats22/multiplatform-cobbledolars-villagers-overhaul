package nl.streats1.cobbledollarsvillagersoverhaul.platform;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Objects;

public final class PlatformNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    
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
        LOGGER.info("PlatformNetwork: Setting client-to-server sender: {}", sender);
        clientToServerSender = Objects.requireNonNull(sender);
        LOGGER.info("PlatformNetwork: Client-to-server sender is now set (non-null: {})", clientToServerSender != null);
    }

    public static void setServerToClientSender(ServerToClientSender sender) {
        LOGGER.info("PlatformNetwork: Setting server-to-client sender: {}", sender);
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
            LOGGER.info("PlatformNetwork: Sending packet to server: {}", payload.type().id());
            sender.send(payload);
        } else {
            LOGGER.error("PlatformNetwork: CANNOT SEND TO SERVER - clientToServerSender is NULL! Packet type: {}", payload.type().id());
            LOGGER.error("PlatformNetwork: This likely means the client-side initialization was never called!");
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerToClientSender sender = serverToClientSender;
        if (sender != null) {
            LOGGER.debug("PlatformNetwork: Sending packet to player {}: {}", player.getName().getString(), payload.type().id());
            sender.send(player, payload);
        } else {
            LOGGER.error("PlatformNetwork: CANNOT SEND TO PLAYER - serverToClientSender is NULL!");
        }
    }
}
