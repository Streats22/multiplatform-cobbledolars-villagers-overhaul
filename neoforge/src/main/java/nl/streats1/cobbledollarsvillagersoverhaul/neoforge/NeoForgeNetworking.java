package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.DefaultShopEditorScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

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
                Objects.requireNonNull(CobbleDollarsShopPayloads.ServerShopConfigSync.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ServerShopConfigSync.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        Config.applyServerShopRuntimeConfig(
                                data.useCobbleDollarsShopUi(),
                                data.villagersAcceptCobbleDollars(),
                                data.useDatapackTrades(),
                                data.useRctTradesOverhaul())));

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
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

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignVillager.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignVillager.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleAssignVillager(sp, data.villagerId());
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopScreenClosed.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopScreenClosed.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleShopScreenClosed(sp, data.villagerId());
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.SaveEntityShop.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.SaveEntityShop.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleSaveEntityShop(sp, data);
                    }
                })
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignModeUpdate.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignModeUpdate.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode.setInMode(data.on()))
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.RctSeriesSelected.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.RctSeriesSelected.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        CobbleDollarsShopScreen.showRctSeriesJourneyOverlay(data.seriesTitleStored()))
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.OpenEditor.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.OpenEditor.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (!"default_shop".equals(data.editorId())) return;
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    mc.setScreen(new DefaultShopEditorScreen(mc.screen));
                })
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.OpenEntityShopEditor.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.OpenEntityShopEditor.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> CobbleDollarsShopScreen.openEntityEditorFromPayload(data))
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
