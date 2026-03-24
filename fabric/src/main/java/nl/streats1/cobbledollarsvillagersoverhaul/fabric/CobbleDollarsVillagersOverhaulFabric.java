package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabric implements ModInitializer {
    private static final long REQUEST_DEBOUNCE_MS = 250L;
    private static final Map<UUID, RequestGate> REQUEST_GATES = new ConcurrentHashMap<>();

    private CobbleDollarsVillagersOverhaulRca mod;

    private static final class RequestGate {
        final int entityId;
        final long atMs;

        RequestGate(int entityId, long atMs) {
            this.entityId = entityId;
            this.atMs = atMs;
        }
    }
    
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
            // Prevent duplicate interaction paths (offhand frequently races on Fabric mod stacks).
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide) {
                long now = System.currentTimeMillis();
                UUID playerId = player.getUUID();
                RequestGate gate = REQUEST_GATES.get(playerId);
                if (gate != null && gate.entityId == entity.getId() && now - gate.atMs < REQUEST_DEBOUNCE_MS) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] Fabric client use-entity: debounced duplicate click player={} entity={} ageMs={}",
                            player.getName().getString(), entity.getId(), now - gate.atMs);
                    // Keep returning FAIL to suppress vanilla packet during debounce window.
                    return InteractionResult.FAIL;
                }

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
                REQUEST_GATES.put(playerId, new RequestGate(entity.getId(), now));
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
