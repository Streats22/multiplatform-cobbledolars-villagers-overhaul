package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

public class CobbleDollarsIntegrationFabric {
    
    public static boolean isModLoaded() {
        try {
            Class.forName("fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static void register() {
        // No-op for now.
    }
}
