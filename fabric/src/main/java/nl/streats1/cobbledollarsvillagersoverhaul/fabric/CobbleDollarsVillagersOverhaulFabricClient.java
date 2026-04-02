package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode;
import nl.streats1.cobbledollarsvillagersoverhaul.client.CycleTradesKeybind;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.DefaultShopEditorScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabricClient implements ClientModInitializer {
    private static final net.minecraft.client.KeyMapping CYCLE_TRADES_KEY = CycleTradesKeybind.create();

    @Override
    public void onInitializeClient() {
        PlatformNetwork.setClientToServerSender(ClientPlayNetworking::send);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FabricMerchantScreenOverlapGuard.clear();
            FabricPendingCustomShopScreen.clear("disconnect");
            ConfigFabric.loadConfig();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FabricPendingCustomShopScreen.onClientTick();
            FabricMerchantScreenOverlapGuard.onEndClientTick(client);
        });

        KeyBindingHelper.registerKeyBinding(CYCLE_TRADES_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CYCLE_TRADES_KEY.consumeClick()) return;
            if (!(client.screen instanceof CobbleDollarsShopScreen screen)) return;
            if (screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox) return;
            screen.onCycleTrades();
        });

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.ServerShopConfigSync.TYPE,
            (payload, context) -> context.client().execute(() -> Config.applyServerShopRuntimeConfig(
                    payload.useCobbleDollarsShopUi(),
                    payload.villagersAcceptCobbleDollars(),
                    payload.useDatapackTrades(),
                    payload.useRctTradesOverhaul())));

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.ShopData.TYPE, 
            (payload, context) -> {
                context.client().execute(() -> {
                    FabricPendingCustomShopScreen.onShopDataReceived(payload.villagerId());
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] ShopData S2C: villagerId={} balance={} buyOffers={} sellOffers={} tradesOffers={} fromConfig={} canCycle={}",
                            payload.villagerId(),
                            payload.balance(),
                            payload.buyOffers() != null ? payload.buyOffers().size() : 0,
                            payload.sellOffers() != null ? payload.sellOffers().size() : 0,
                            payload.tradesOffers() != null ? payload.tradesOffers().size() : 0,
                            payload.buyOffersFromConfig(),
                            payload.canCycleTrades());
                    var client = context.client();
                    boolean shopAlreadyOpen = client.screen instanceof CobbleDollarsShopScreen s
                            && s.shopTargetEntityId() == payload.villagerId();
                    if (!shopAlreadyOpen) {
                        FabricMerchantScreenOverlapGuard.arm(payload);
                    }
                    CobbleDollarsShopScreen.openFromPayload(
                        payload.villagerId(), 
                        payload.balance(), 
                        payload.buyOffers(), 
                        payload.sellOffers(),
                        payload.tradesOffers(),
                        payload.buyOffersFromConfig(),
                        payload.canCycleTrades()
                    );
                    if (!shopAlreadyOpen) {
                        FabricMerchantScreenOverlapGuard.scheduleDeferredRecheck(client);
                    }
                    CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                            "[shop] ShopData handler: after openFromPayload screen={}",
                            client.screen != null ? client.screen.getClass().getSimpleName() : "null");
                });
            });
            
        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.BalanceUpdate.TYPE, 
            (payload, context) -> {
                context.client().execute(() -> {
                    CobbleDollarsShopScreen.updateBalanceFromServer(
                        payload.villagerId(), 
                        payload.balance()
                    );
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.AssignModeUpdate.TYPE,
                (payload, context) -> context.client().execute(() -> ClientAssignMode.setInMode(payload.on())));

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.RctSeriesSelected.TYPE,
                (payload, context) -> context.client().execute(() ->
                        CobbleDollarsShopScreen.showRctSeriesJourneyOverlay(payload.seriesTitleStored())));

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.OpenEditor.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (!"default_shop".equals(payload.editorId())) return;
                    var client = context.client();
                    client.setScreen(new DefaultShopEditorScreen(client.screen));
                }));

        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.OpenEntityShopEditor.TYPE,
                (payload, context) -> context.client().execute(() ->
                        CobbleDollarsShopScreen.openEntityEditorFromPayload(payload)));
    }
}
