package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

public class VillagerCobbleDollarsHandlerFabric {
    
    public static void registerFabric() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
        });
    }
}
