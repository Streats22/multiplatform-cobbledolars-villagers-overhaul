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
                Objects.requireNonNull(CobbleDollarsShopPayloads.ServerShopConfigSync.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ServerShopConfigSync.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    Config.applyServerShopRuntimeConfig(
                            data.useCobbleDollarsShopUi(),
                            data.villagersAcceptCobbleDollars(),
                            data.useDatapackTrades(),
                            data.useRctTradesOverhaul(),
                            data.emeraldRateCdPerEmerald(),
                            data.syncCobbleDollarsBankRate());
                    nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopLabels.applySynced(
                            new nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopLabels.Values(
                                    data.shopTitle(), data.buyTab(), data.sellTab(), data.tradesTab()));
                }));

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    NeoForgePendingCustomShopMerchantSuppress.onShopDataReceived(data.villagerId());
                        CobbleDollarsShopScreen.openFromPayload(data);
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
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestAdminConfig.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestAdminConfig.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService.handleRequest(sp, data.kind());
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.SaveAdminConfig.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.SaveAdminConfig.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService.handleSave(sp, data.kind(), data.json());
                    }
                })
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.AdminConfigData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.AdminConfigData.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient.onData(data.kind(), data.json()))
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestAssignedList.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestAssignedList.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleRequestAssignedList(sp);
                    }
                })
        );

        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.UnassignByUuid.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.UnassignByUuid.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CobbleDollarsShopPayloadHandlers.handleUnassignByUuid(sp, data.uuid());
                    }
                })
        );

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignedList.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignedList.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignedVillagers.set(data.uuids(), data.labels()))
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

        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignModeUpdate.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.AssignModeUpdate.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode.setInMode(data.on()))
        );
    }

    private static void handleRequestShopData(CobbleDollarsShopPayloads.RequestShopData data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            CobbleDollarsShopPayloadHandlers.handleRequestShopData(serverPlayer, data.villagerId());
        });
    }
}
