package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerCobbleDollarsHandler;

public class VillagerCobbleDollarsHandlerFabric {
    
    public static void registerFabric() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            // Handle entity load events if needed
        });
        
        // Register trade event handler
        // Note: Fabric doesn't have a direct TradeWithVillagerEvent, so we need to use a different approach
        // This would typically be handled through a mixin or alternative event system
    }
}
