package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulNeoForgeClient {
    
    public static void initializeClient(FMLClientSetupEvent event) {
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== NeoForge Client Setup ===");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Registering client-to-server sender for PlatformNetwork");
        PlatformNetwork.setClientToServerSender(PacketDistributor::sendToServer);
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Client-to-server sender registered successfully!");
    }
}
