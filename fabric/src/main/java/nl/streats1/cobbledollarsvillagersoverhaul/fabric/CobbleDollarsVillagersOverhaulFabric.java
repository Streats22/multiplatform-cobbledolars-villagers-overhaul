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
     * After sending {@link CobbleDollarsShopPayloads.RequestShopData}, return {@link InteractionResult#FAIL} so the
     * vanilla use-entity packet is not also sent (avoids duplicate server handling and races with the merchant UI).
     * Server still returns {@link InteractionResult#SUCCESS} when a vanilla packet is processed, to cancel trading.
     */
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) {
                boolean handled = mod.onEntityInteract(entity, true, player.isShiftKeyDown(), () -> {});
                if (!handled) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] Fabric client use-entity: not handled (PASS), entity={} id={}",
                            entity.getType().getDescriptionId(), entity.getId());
                    return InteractionResult.PASS;
                }
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric client use-entity: sending RequestShopData, entity={} id={}, result=FAIL (no vanilla packet)",
                        entity.getType().getDescriptionId(), entity.getId());
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
                return InteractionResult.FAIL;
            }
            boolean handledServer = mod.onEntityInteract(entity, false, player.isShiftKeyDown(), () -> {});
            if (!handledServer) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric server use-entity: not handled (PASS), entity={} id={}",
                        entity.getType().getDescriptionId(), entity.getId());
                return InteractionResult.PASS;
            }
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] Fabric server use-entity: cancel vanilla merchant (SUCCESS), entity={} id={}",
                    entity.getType().getDescriptionId(), entity.getId());
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
}
