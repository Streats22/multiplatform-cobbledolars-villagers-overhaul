package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabric implements ModInitializer {

    private CobbleDollarsVillagersOverhaulRca mod;
    
    @Override
    public void onInitialize() {
        mod = new CobbleDollarsVillagersOverhaulRca();
        registerEvents();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                VillagerShopCommand.register(dispatcher));

        FabricNetworking.register();
        ConfigFabric.loadConfig();
    }

    /**
     * Client returns {@link InteractionResult#FAIL} after sending {@code RequestShopData} so the vanilla
     * use-entity packet is not also sent (SUCCESS would duplicate server handling and race merchant vs shop UI).
     * Server still returns SUCCESS when a vanilla interaction packet is processed, to cancel {@code MerchantMenu}.
     */
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) {
                boolean handled = mod.onEntityInteract(entity, true, player.isShiftKeyDown(), () -> {});
                if (!handled) return InteractionResult.PASS;
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
                return InteractionResult.FAIL;
            }
            boolean handledServer = mod.onEntityInteract(entity, false, player.isShiftKeyDown(), () -> {});
            if (!handledServer) return InteractionResult.PASS;
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
}
