package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void register() {
        PlatformNetwork.setServerToClientSender(ServerPlayNetworking::send);

        PayloadTypeRegistry.playC2S().register(CobbleDollarsShopPayloads.RequestShopData.TYPE, CobbleDollarsShopPayloads.RequestShopData.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CobbleDollarsShopPayloads.BuyWithCobbleDollars.TYPE, CobbleDollarsShopPayloads.BuyWithCobbleDollars.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CobbleDollarsShopPayloads.SellForCobbleDollars.TYPE, CobbleDollarsShopPayloads.SellForCobbleDollars.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(CobbleDollarsShopPayloads.ShopData.TYPE, CobbleDollarsShopPayloads.ShopData.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CobbleDollarsShopPayloads.BalanceUpdate.TYPE, CobbleDollarsShopPayloads.BalanceUpdate.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.RequestShopData.TYPE, (payload, context) -> {
            if (context.player() instanceof ServerPlayer sp) {
                context.server().execute(() -> CobbleDollarsShopPayloadHandlers.handleRequestShopData(sp, payload.villagerId()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.BuyWithCobbleDollars.TYPE, (payload, context) -> {
            if (context.player() instanceof ServerPlayer sp) {
                context.server().execute(() -> CobbleDollarsShopPayloadHandlers.handleBuy(sp, payload.villagerId(), payload.offerIndex(), payload.quantity(), payload.fromConfigShop()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CobbleDollarsShopPayloads.SellForCobbleDollars.TYPE, (payload, context) -> {
            if (context.player() instanceof ServerPlayer sp) {
                context.server().execute(() -> CobbleDollarsShopPayloadHandlers.handleSell(sp, payload.villagerId(), payload.offerIndex(), payload.quantity()));
            }
        });
    }
}
