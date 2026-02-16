package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.command.CobbleMerchantCommands;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

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

        // Register shop menu type and opener (so server can open menu; client gets slots for moving items)
        FabricMenuRegistration.register();

        // Register /cvm and /cobblevillmerch commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CobbleMerchantCommands.register(dispatcher));
        
        // Load config
        Config.loadConfig();
    }

    /**
     * Entity-based shop open (CobbleDollars-style): right-click villager/wandering trader opens our shop instead of vanilla.
     */
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            boolean isClient = world.isClientSide;
            boolean handled = mod.onEntityInteract(entity, isClient, player.isShiftKeyDown(), () -> {
            }, entity::getId);
            if (!handled) return InteractionResult.PASS;

            if (mod.shouldSendShopRequest(isClient)) {
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
            }
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
    
    // Networking is registered in FabricNetworking.
}
