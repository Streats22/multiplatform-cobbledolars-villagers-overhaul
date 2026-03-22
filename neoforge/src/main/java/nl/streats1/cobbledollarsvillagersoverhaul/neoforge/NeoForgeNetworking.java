package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.Objects;

public final class NeoForgeNetworking {
    private NeoForgeNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        PlatformNetwork.setServerToClientSender(PacketDistributor::sendToPlayer);
        modEventBus.addListener(NeoForgeNetworking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestShopData.STREAM_CODEC),
                (data, context) -> handleRequestShopData(data, context)
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
                            CobbleDollarsVillagersOverhaulRca.LOGGER.debug("[shop] ShopData S2C ignored: USE_COBBLEDOLLARS_SHOP_UI=false");
                            return;
                        }
                        CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                                "[shop] ShopData S2C: villagerId={} balance={} buyOffers={} sellOffers={} tradesOffers={} fromConfig={} canCycle={}",
                                data.villagerId(),
                                data.balance(),
                                data.buyOffers() != null ? data.buyOffers().size() : 0,
                                data.sellOffers() != null ? data.sellOffers().size() : 0,
                                data.tradesOffers() != null ? data.tradesOffers().size() : 0,
                                data.buyOffersFromConfig(),
                                data.canCycleTrades());
                        CobbleDollarsShopScreen.openFromPayload(
                                data.villagerId(), data.balance(), data.buyOffers(), data.sellOffers(), data.tradesOffers(), data.buyOffersFromConfig(), data.canCycleTrades());
                })
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.BalanceUpdate.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.BalanceUpdate.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        CobbleDollarsShopScreen.updateBalanceFromServer(
                                data.villagerId(), data.balance()))
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleBuy(sp, data.villagerId(), data.offerIndex(), data.quantity(), data.fromConfigShop(), data.tab(), data.selectedSeries());
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.CycleTrades.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.CycleTrades.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleCycleTrades(sp, data.villagerId());
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.SellForCobbleDollars.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.SellForCobbleDollars.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleSell(sp, data.villagerId(), data.offerIndex(), data.quantity());
                    }
                })
        );
    }

    private static void handleRequestShopData(CobbleDollarsShopPayloads.RequestShopData data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.warn("[shop] RequestShopData: player is not ServerPlayer, ignoring");
                return;
            }
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] RequestShopData received (NeoForge): player={} villagerEntityId={}",
                    serverPlayer.getName().getString(), data.villagerId());
            CobbleDollarsShopPayloadHandlers.handleRequestShopData(serverPlayer, data.villagerId());
        });
    }
}
