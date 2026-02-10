package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.neoforged.fml.ModList;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;

public class CobbleDollarsIntegrationNeoForge {
    
    public static boolean isModLoaded() {
        return ModList.get().isLoaded("cobbledollars");
    }
    
    public static void register() {
        // NeoForge-specific registration if needed
    }
}
