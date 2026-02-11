package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        PlatformNetwork.setClientToServerSender(ClientPlayNetworking::send);

        // Register client-side networking
        ClientPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.ShopData.TYPE, 
            (payload, context) -> {
                context.client().execute(() -> {
                    CobbleDollarsShopScreen.openFromPayload(
                        payload.villagerId(), 
                        payload.balance(), 
                        payload.buyOffers(), 
                        payload.sellOffers(),
                        payload.tradesOffers(),
                        payload.buyOffersFromConfig()
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
