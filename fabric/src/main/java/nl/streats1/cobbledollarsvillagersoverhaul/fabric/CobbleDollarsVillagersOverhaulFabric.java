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
     * Villager shop open: client sends {@link CobbleDollarsShopPayloads.RequestShopData}; server must also
     * return SUCCESS here so vanilla does not open {@code MerchantMenu} in parallel (avoids flicker /
     * instant close after other CobbleDollars UIs like CobbleMerchant). NeoForge already cancels via
     * {@code PlayerInteractEvent}; Fabric previously only handled the logical client.
     */
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) {
                boolean handled = mod.onEntityInteract(entity, true, player.isShiftKeyDown(), () -> {});
                if (!handled) return InteractionResult.PASS;
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
                return InteractionResult.SUCCESS;
            }
            boolean handledServer = mod.onEntityInteract(entity, false, player.isShiftKeyDown(), () -> {});
            if (!handledServer) return InteractionResult.PASS;
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
}
