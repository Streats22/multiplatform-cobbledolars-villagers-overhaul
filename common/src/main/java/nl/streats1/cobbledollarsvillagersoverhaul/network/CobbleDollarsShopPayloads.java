package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("null")
public final class CobbleDollarsShopPayloads {

    private static final String PREFIX = "cobbledollars_shop/";
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT = (StreamCodec) ByteBufCodecs.VAR_INT;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, Long> VAR_LONG = (StreamCodec) ByteBufCodecs.VAR_LONG;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION = (StreamCodec)
            ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString);
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
     * - {@code seriesName} is the translatable key for the title (e.g. "series.rctmod.bdsp.title")
     * - {@code seriesTooltip} is the translatable key for the description (e.g. "series.rctmod.bdsp.description")
     * - {@code seriesDifficulty} is the difficulty rating (can be fractional for half stars, e.g. 4.5)
     * - {@code seriesCompleted} is the number of times the player has completed this series
     */
    public record ShopOfferEntry(ItemStack result,
                                 int emeraldCount,
                                 ItemStack costB,
                                 boolean directPrice,
                                 String seriesId,
                                 String seriesName,
                                 String seriesTooltip,
                                 float seriesDifficulty,
                                 int seriesCompleted) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopOfferEntry> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, ShopOfferEntry entry) {
                        // Defensive null checks for encoding
                        ItemStack result = entry.result();
                        if (result == null || result.isEmpty()) result = new ItemStack(Items.BREAD);

                        ItemStack costB = entry.costB();
                        // If costB is null or empty, use a placeholder item that we'll detect on decode
                        // Using STONE with count 1 as a sentinel value for "no costB"
                        if (costB == null || costB.isEmpty() || costB.is(Items.AIR)) {
                            costB = new ItemStack(Items.STONE);  // Sentinel value
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
                    }

                    @Override
                    public ShopOfferEntry decode(RegistryFriendlyByteBuf buf) {
                        ItemStack result = ITEM_STACK.decode(buf);
                        int emeraldCount = VAR_INT.decode(buf);
                        ItemStack costB = ITEM_STACK.decode(buf);
                        // Check for sentinel value (STONE with count 1 means "no costB")
                        if (costB != null && costB.is(Items.STONE) && costB.getCount() == 1) {
                            costB = ItemStack.EMPTY;
                        }
                        boolean directPrice = BOOL.decode(buf);
                        String seriesId = STRING_UTF8.decode(buf);
                        String seriesName = STRING_UTF8.decode(buf);
                        String seriesTooltip = STRING_UTF8.decode(buf);
                        float seriesDifficulty = buf.readFloat();
                        int seriesCompleted = VAR_INT.decode(buf);
                        return new ShopOfferEntry(
                                result,
                                emeraldCount,
                                costB,
                                directPrice,
                                seriesId != null ? seriesId : "",
                                seriesName != null ? seriesName : "",
                                seriesTooltip != null ? seriesTooltip : "",
                                seriesDifficulty,
                                seriesCompleted);
                    }
                };

        public boolean hasCostB() {
            return costB != null && !costB.isEmpty() && !costB.is(Items.AIR);
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
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        ShopData::villagerId,
                        VAR_LONG,
                        ShopData::balance,
                        OFFERS_LIST_CODEC,
                        ShopData::buyOffers,
                        OFFERS_LIST_CODEC,
                        ShopData::sellOffers,
                        OFFERS_LIST_CODEC,
                        ShopData::tradesOffers,
                        BOOL,
                        ShopData::buyOffersFromConfig,
                        BOOL,
                        ShopData::canCycleTrades,
                        (villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades) -> new ShopData(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(balance),
                                Objects.requireNonNull(buyOffers),
                                Objects.requireNonNull(sellOffers),
                                Objects.requireNonNull(tradesOffers),
                                buyOffersFromConfig,
                                canCycleTrades)
                ));

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
}
