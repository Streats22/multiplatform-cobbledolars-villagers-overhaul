package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.command.CvmCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CobbleDollarsVillagersOverhaulFabric implements ModInitializer {
    private static final long REQUEST_DEBOUNCE_MS = 250L;
    private static final Map<UUID, RequestGate> REQUEST_GATES = new ConcurrentHashMap<>();

    private CobbleDollarsVillagersOverhaulRca mod;

    private record RequestGate(int entityId, long atMs) {
    }

    @Override
    public void onInitialize() {
        mod = new CobbleDollarsVillagersOverhaulRca();
        registerEvents();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VillagerShopCommand.register(dispatcher);
            CvmCommand.register(dispatcher);
        });

        // Register networking
        FabricNetworking.register();
        ConfigFabric.loadConfig();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player instanceof ServerPlayer sp) {
                CobbleDollarsShopPayloadHandlers.sendServerShopConfigTo(sp);
            }
        });
    }

    /**
     * After sending {@link CobbleDollarsShopPayloads.RequestShopData}, return {@link InteractionResult#CONSUME} so the
     * vanilla use-entity packet still reaches the dedicated server: our {@link UseEntityCallback} there cancels the
     * default merchant open, which avoids races where the client never sent a use packet (see FAIL) but another path
     * still opened or replaced the GUI on multiplayer.
     * Debounced repeat clicks still return {@link InteractionResult#FAIL} so no extra use packet is sent in that window.
     */
    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Prevent duplicate interaction paths (offhand frequently races on Fabric mod stacks).
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide
                    && nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode.isInMode()
                    && player.isShiftKeyDown()
                    && entity instanceof net.minecraft.world.entity.npc.Villager) {
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.AssignVillager(entity.getId()));
                return InteractionResult.SUCCESS;
            }
            if (world.isClientSide) {
                long now = System.currentTimeMillis();
                UUID playerId = player.getUUID();
                RequestGate gate = REQUEST_GATES.get(playerId);
                if (gate != null && gate.entityId == entity.getId() && now - gate.atMs < REQUEST_DEBOUNCE_MS) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] Fabric client use-entity: debounced duplicate click player={} entity={} ageMs={}",
                            player.getName().getString(), entity.getId(), now - gate.atMs);
                    return InteractionResult.FAIL;
                }

                boolean handledClient = mod.onEntityInteract(entity, true, player.isShiftKeyDown(), () -> {});
                if (!handledClient) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] Fabric client use-entity: not handled (PASS), entity={} id={}",
                            entity.getType().getDescriptionId(), entity.getId());
                    return InteractionResult.PASS;
                }
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric client use-entity: sending RequestShopData, entity={} id={}, result=CONSUME",
                        entity.getType().getDescriptionId(), entity.getId());
                REQUEST_GATES.put(playerId, new RequestGate(entity.getId(), now));
                FabricPendingCustomShopScreen.beginAwaitingShopData(entity.getId());
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
                return InteractionResult.CONSUME;
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
