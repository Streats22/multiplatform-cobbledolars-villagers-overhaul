package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import nl.streats1.cobbledollarsvillagersoverhaul.client.CycleTradesKeybind;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabricClient implements ClientModInitializer {
    private static final net.minecraft.client.KeyMapping CYCLE_TRADES_KEY = CycleTradesKeybind.create();

    @Override
    public void onInitializeClient() {
        PlatformNetwork.setClientToServerSender(ClientPlayNetworking::send);

        KeyBindingHelper.registerKeyBinding(CYCLE_TRADES_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CYCLE_TRADES_KEY.consumeClick()) return;
            if (!(client.screen instanceof CobbleDollarsShopScreen screen)) return;
            if (screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox) return;
            screen.onCycleTrades();
        });

        // Register client-side networking
        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.ShopData.TYPE, 
            (payload, context) -> {
                context.client().execute(() -> {
                    if (!Config.USE_COBBLEDOLLARS_SHOP_UI) return;
                    CobbleDollarsShopScreen.openFromPayload(
                        payload.villagerId(), 
                        payload.balance(), 
                        payload.buyOffers(), 
                        payload.sellOffers(),
                        payload.tradesOffers(),
                        payload.buyOffersFromConfig(),
                        payload.canCycleTrades()
                    );
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
    }
}
