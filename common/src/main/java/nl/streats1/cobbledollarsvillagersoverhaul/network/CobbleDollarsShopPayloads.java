package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

@SuppressWarnings("null")
public final class CobbleDollarsShopPayloads {

    private static final String PREFIX = "cobbledollars_shop/";
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT = (StreamCodec) ByteBufCodecs.VAR_INT;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, Long> VAR_LONG = (StreamCodec) ByteBufCodecs.VAR_LONG;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL = (StreamCodec) ByteBufCodecs.BOOL;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_UTF8 = (StreamCodec) ByteBufCodecs.STRING_UTF8;
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = ItemStack.STREAM_CODEC;

    /**
     * One logical shop entry.
     * <p>
     * For RCT trainer association trades:
     * - {@code seriesId} is the series identifier (e.g. "bdsp", "radicalred") for server communication
     * - {@code seriesName} is the title: a translation key, or {@code literal:...} for plain text from datapack JSON
     * - {@code seriesTooltip} is the description, same convention as {@code seriesName}
     * - {@code seriesDifficulty} is the difficulty rating (can be fractional for half stars, e.g. 4.5)
     * - {@code seriesCompleted} is the number of times the player has completed this series
     * <p>
     * Trades tab (item-for-item): {@code result} is drawn on the left = merchant {@code getCostA()} (first input).
     * {@code costB} is after the arrow = merchant {@code getResult()} (output). {@code itemTradeSecondary} is
     * merchant {@code getCostB()} when the trade uses two inputs; otherwise {@link ItemStack#EMPTY}.
     * Buy/Sell entries use {@code result}/{@code costB} with normal buy/sell meaning.
     * {@code categoryName} is used for default/config shop buy tabs (empty for villager trades).
     */
    public record ShopOfferEntry(ItemStack result,
                                 int emeraldCount,
                                 ItemStack costB,
                                 boolean directPrice,
                                 String seriesId,
                                 String seriesName,
                                 String seriesTooltip,
                                 float seriesDifficulty,
                                 int seriesCompleted,
                                 ItemStack itemTradeSecondary,
                                 String categoryName) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopOfferEntry> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, ShopOfferEntry entry) {
                        ItemStack result = entry.result();
                        if (result == null || result.isEmpty()) result = new ItemStack(Items.BREAD);

                        ItemStack costB = entry.costB();
                        if (costB == null || costB.isEmpty() || costB.is(Items.AIR)) {
                            costB = new ItemStack(Items.STONE);
                        }

                        ItemStack sec = entry.itemTradeSecondary();
                        if (sec == null || sec.isEmpty() || sec.is(Items.AIR)) {
                            sec = new ItemStack(Items.STONE);
                        }

                        ITEM_STACK.encode(buf, result);
                        VAR_INT.encode(buf, entry.emeraldCount());
                        ITEM_STACK.encode(buf, costB);
                        BOOL.encode(buf, entry.directPrice());
                        STRING_UTF8.encode(buf, entry.seriesId() != null ? entry.seriesId() : "");
                        STRING_UTF8.encode(buf, entry.seriesName() != null ? entry.seriesName() : "");
                        STRING_UTF8.encode(buf, entry.seriesTooltip() != null ? entry.seriesTooltip() : "");
                        buf.writeFloat(entry.seriesDifficulty());
                        VAR_INT.encode(buf, entry.seriesCompleted());
                        ITEM_STACK.encode(buf, sec);
                        STRING_UTF8.encode(buf, entry.categoryName() != null ? entry.categoryName() : "");
                    }

                    @Override
                    public ShopOfferEntry decode(RegistryFriendlyByteBuf buf) {
                        ItemStack result = ITEM_STACK.decode(buf);
                        int emeraldCount = VAR_INT.decode(buf);
                        ItemStack costB = ITEM_STACK.decode(buf);
                        if (costB != null && costB.is(Items.STONE) && costB.getCount() == 1) {
                            costB = ItemStack.EMPTY;
                        }
                        boolean directPrice = BOOL.decode(buf);
                        String seriesId = STRING_UTF8.decode(buf);
                        String seriesName = STRING_UTF8.decode(buf);
                        String seriesTooltip = STRING_UTF8.decode(buf);
                        float seriesDifficulty = buf.readFloat();
                        int seriesCompleted = VAR_INT.decode(buf);
                        ItemStack itemTradeSecondary = ITEM_STACK.decode(buf);
                        if (itemTradeSecondary != null && itemTradeSecondary.is(Items.STONE)
                                && itemTradeSecondary.getCount() == 1) {
                            itemTradeSecondary = ItemStack.EMPTY;
                        }
                        String categoryName = STRING_UTF8.decode(buf);
                        return new ShopOfferEntry(
                                result,
                                emeraldCount,
                                costB,
                                directPrice,
                                seriesId != null ? seriesId : "",
                                seriesName != null ? seriesName : "",
                                seriesTooltip != null ? seriesTooltip : "",
                                seriesDifficulty,
                                seriesCompleted,
                                itemTradeSecondary != null ? itemTradeSecondary : ItemStack.EMPTY,
                                categoryName != null ? categoryName : "");
                    }
                };

        public boolean hasCostB() {
            return costB != null && !costB.isEmpty() && !costB.is(Items.AIR);
        }

        /** Second merchant input for Trades-tab barters; empty for Buy/Sell. */
        public boolean hasItemTradeSecondary() {
            return itemTradeSecondary != null && !itemTradeSecondary.isEmpty() && !itemTradeSecondary.is(Items.AIR);
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ShopOfferEntry>> OFFERS_LIST_CODEC =
            StreamCodec.of(
                    (buf, list) -> {
                        VAR_INT.encode(buf, list.size());
                        for (ShopOfferEntry e : list) ShopOfferEntry.STREAM_CODEC.encode(buf, e);
                    },
                    buf -> {
                        int n = VAR_INT.decode(buf);
                        List<ShopOfferEntry> out = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) out.add(ShopOfferEntry.STREAM_CODEC.decode(buf));
                        return out;
                    }
            );

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobbleDollarsVillagersOverhaulRca.MOD_ID, PREFIX + path);
    }

    public record RequestShopData(int villagerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestShopData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("request_shop_data")));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestShopData> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        RequestShopData::villagerId,
                        id -> new RequestShopData(Objects.requireNonNull(id))
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig, boolean canCycleTrades) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShopData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("shop_data")));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopData> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            VAR_INT.encode(buf, data.villagerId());
                            VAR_LONG.encode(buf, data.balance());
                            OFFERS_LIST_CODEC.encode(buf, data.buyOffers());
                            OFFERS_LIST_CODEC.encode(buf, data.sellOffers());
                            OFFERS_LIST_CODEC.encode(buf, data.tradesOffers());
                            BOOL.encode(buf, data.buyOffersFromConfig());
                            BOOL.encode(buf, data.canCycleTrades());
                        },
                        buf -> new ShopData(
                                VAR_INT.decode(buf),
                                VAR_LONG.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf))
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server → client: authoritative shop flags so multiplayer clients match the dedicated server
     * (singleplayer already shares one config file; remote clients otherwise read local config only).
     */
    public record ServerShopConfigSync(
            boolean useCobbleDollarsShopUi,
            boolean villagersAcceptCobbleDollars,
            boolean useDatapackTrades,
            boolean useRctTradesOverhaul
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerShopConfigSync> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("server_shop_config_sync")));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerShopConfigSync> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            BOOL.encode(buf, data.useCobbleDollarsShopUi());
                            BOOL.encode(buf, data.villagersAcceptCobbleDollars());
                            BOOL.encode(buf, data.useDatapackTrades());
                            BOOL.encode(buf, data.useRctTradesOverhaul());
                        },
                        buf -> new ServerShopConfigSync(
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf))
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BalanceUpdate(int villagerId, long balance) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BalanceUpdate> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("balance_update")));
        public static final StreamCodec<RegistryFriendlyByteBuf, BalanceUpdate> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        BalanceUpdate::villagerId,
                        VAR_LONG,
                        BalanceUpdate::balance,
                        (villagerId, balance) -> new BalanceUpdate(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(balance))
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BuyWithCobbleDollars(int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab, String selectedSeries) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BuyWithCobbleDollars> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("buy")));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuyWithCobbleDollars> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        BuyWithCobbleDollars::villagerId,
                        VAR_INT,
                        BuyWithCobbleDollars::offerIndex,
                        VAR_INT,
                        BuyWithCobbleDollars::quantity,
                        BOOL,
                        BuyWithCobbleDollars::fromConfigShop,
                        VAR_INT,
                        BuyWithCobbleDollars::tab,
                        ByteBufCodecs.STRING_UTF8,
                        BuyWithCobbleDollars::selectedSeries,
                        (villagerId, offerIndex, quantity, fromConfigShop, tab, selectedSeries) -> new BuyWithCobbleDollars(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(offerIndex),
                                Objects.requireNonNull(quantity),
                                fromConfigShop,
                                Objects.requireNonNull(tab),
                                selectedSeries != null ? selectedSeries : "")
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CycleTrades(int villagerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CycleTrades> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("cycle_trades")));
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleTrades> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        CycleTrades::villagerId,
                        CycleTrades::new
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SellForCobbleDollars(int villagerId, int offerIndex, int quantity) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SellForCobbleDollars> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("sell")));
        public static final StreamCodec<RegistryFriendlyByteBuf, SellForCobbleDollars> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        SellForCobbleDollars::villagerId,
                        VAR_INT,
                        SellForCobbleDollars::offerIndex,
                        VAR_INT,
                        SellForCobbleDollars::quantity,
                        (villagerId, offerIndex, quantity) -> new SellForCobbleDollars(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(offerIndex),
                                Objects.requireNonNull(quantity)
                        )
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Client -> Server: Player dismissed the CobbleDollars shop (Esc or close). On integrated singleplayer / LAN,
     * we avoid {@link net.minecraft.world.entity.npc.AbstractVillager#setTradingPlayer} during trades so the GUI
     * stays open; this clears the merchant session when the UI actually closes.
     */
    public record ShopScreenClosed(int villagerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShopScreenClosed> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("shop_screen_closed")));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopScreenClosed> STREAM_CODEC =
                StreamCodec.composite(VAR_INT, ShopScreenClosed::villagerId, ShopScreenClosed::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Client -> Server: Assign config shop to villager (sent when shift+left-click in assign mode).
     */
    public record AssignVillager(int villagerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AssignVillager> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("assign_villager")));
        public static final StreamCodec<RegistryFriendlyByteBuf, AssignVillager> STREAM_CODEC =
                StreamCodec.composite(VAR_INT, AssignVillager::villagerId, AssignVillager::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> Client: Sync assign mode on/off.
     */
    public record AssignModeUpdate(boolean on) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AssignModeUpdate> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("assign_mode_update")));
        public static final StreamCodec<RegistryFriendlyByteBuf, AssignModeUpdate> STREAM_CODEC =
                StreamCodec.composite(BOOL, AssignModeUpdate::on, AssignModeUpdate::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> Client: RCT series trade completed and the player's current series was set.
     * Sent only after the server has applied the series selection so the client can show the
     * "A new journey + <series>" overlay at the correct time.
     *
     * @param seriesTitleStored translation key or {@code literal:...} (same convention as ShopOfferEntry.seriesName)
     */
    public record RctSeriesSelected(String seriesTitleStored) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RctSeriesSelected> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("rct_series_selected")));
        public static final StreamCodec<RegistryFriendlyByteBuf, RctSeriesSelected> STREAM_CODEC =
                StreamCodec.composite(STRING_UTF8, RctSeriesSelected::seriesTitleStored, RctSeriesSelected::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
