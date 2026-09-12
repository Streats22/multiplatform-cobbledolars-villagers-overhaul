package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.command.CvmCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabric implements ModInitializer {
    private static final long REQUEST_DEBOUNCE_MS = 250L;
    private static final Map<UUID, RequestGate> REQUEST_GATES = new ConcurrentHashMap<>();

    private CobbleDollarsVillagersOverhaulRca mod;

    private record RequestGate(int entityId, long atMs) {
    }

    @Override
    public void onInitialize() {
        Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CobbleDollarsVillagersOverhaulRca.MOD_ID, "cobbledollar_sign"),
                new Item(new Item.Properties())
        );

        mod = new CobbleDollarsVillagersOverhaulRca();
        registerEvents();

        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VillagerShopCommand.register(dispatcher);
            CvmCommand.register(dispatcher);
        });

        
        FabricNetworking.register();
        ConfigFabric.loadConfig();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player instanceof ServerPlayer sp) {
                CobbleDollarsShopPayloadHandlers.sendServerShopConfigTo(sp);
            }
        });

        
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player instanceof ServerPlayer sp) {
                CobbleDollarsShopPayloadHandlers.handlePlayerDisconnect(sp);
            }
        });
    }

    private void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            
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
                    return InteractionResult.FAIL;
                }

                boolean handledClient = mod.onEntityInteract(entity, true, player.isShiftKeyDown(),
                        player.getItemInHand(hand), () -> {});
                if (!handledClient) {
                    return InteractionResult.PASS;
                }
                REQUEST_GATES.put(playerId, new RequestGate(entity.getId(), now));
                FabricPendingCustomShopScreen.beginAwaitingShopData(entity.getId());
                PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(entity.getId()));
                return InteractionResult.CONSUME;
            }
            boolean handledServer = mod.onEntityInteract(entity, false, player.isShiftKeyDown(),
                    player.getItemInHand(hand), () -> {});
            if (!handledServer) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        });
        
        VillagerCobbleDollarsHandlerFabric.registerFabric();
    }
}
