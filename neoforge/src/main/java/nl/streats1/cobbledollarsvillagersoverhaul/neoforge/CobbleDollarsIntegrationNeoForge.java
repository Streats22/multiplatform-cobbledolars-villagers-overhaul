package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.neoforged.fml.ModList;

public class CobbleDollarsIntegrationNeoForge {
    
    public static boolean isModLoaded() {
        return ModList.get().isLoaded("cobbledollars");
    }
    
    public static void register() {
    }
}
