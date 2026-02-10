package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import net.minecraft.world.InteractionResult;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;

public class CobbleDollarsVillagersOverhaulFabric implements ModInitializer {

    private CobbleDollarsVillagersOverhaulRca mod;
    
    @Override
    public void onInitialize() {
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Initializing CobbleDollars Villagers Overhaul (Fabric)");
        
        // Initialize common mod
        mod = new CobbleDollarsVillagersOverhaulRca();
        
        // Register Fabric-specific events
        registerEvents();
        
        // Register networking
        FabricNetworking.register();
        
        // Load config
        Config.loadConfig();
    }
    
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide) {
                return InteractionResult.PASS;
            }

            boolean handled = mod.onEntityInteract(entity, true, player.isShiftKeyDown(), () -> {}, entity::getId);
            if (!handled) return InteractionResult.PASS;

            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
    
    // Networking is registered in FabricNetworking.
}
