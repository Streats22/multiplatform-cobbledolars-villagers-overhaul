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
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = ItemStack.STREAM_CODEC;

    public record ShopOfferEntry(ItemStack result, int emeraldCount, ItemStack costB, boolean directPrice, String seriesName) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopOfferEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ITEM_STACK,
                        ShopOfferEntry::result,
                        VAR_INT,
                        ShopOfferEntry::emeraldCount,
                        // Use optional ItemStack codec for costB to handle empty items
                        StreamCodec.of(
                                (buf, stack) -> {
                                    boolean hasCostB = stack != null && !stack.isEmpty();
                                    BOOL.encode(buf, hasCostB);
                                    if (hasCostB) {
                                        ITEM_STACK.encode(buf, stack);
                                    }
                                },
                                buf -> {
                                    boolean hasCostB = BOOL.decode(buf);
                                    return hasCostB ? ITEM_STACK.decode(buf) : ItemStack.EMPTY;
                                }
                        ),
                        ShopOfferEntry::costB,
                        BOOL,
                        ShopOfferEntry::directPrice,
                        ByteBufCodecs.STRING_UTF8,
                        ShopOfferEntry::seriesName,
                        (result, emeraldCount, costB, directPrice, seriesName) -> new ShopOfferEntry(
                                Objects.requireNonNull(result),
                                Objects.requireNonNull(emeraldCount),
                                Objects.requireNonNull(costB),
                                directPrice,
                                seriesName != null ? seriesName : "")
                );

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

    public record ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig) implements CustomPacketPayload {
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
                        (villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig) -> new ShopData(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(balance),
                                Objects.requireNonNull(buyOffers),
                                Objects.requireNonNull(sellOffers),
                                Objects.requireNonNull(tradesOffers),
                                buyOffersFromConfig)
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

    public record BuyWithCobbleDollars(int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab) implements CustomPacketPayload {
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
                        (villagerId, offerIndex, quantity, fromConfigShop, tab) -> new BuyWithCobbleDollars(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(offerIndex),
                                Objects.requireNonNull(quantity),
                                fromConfigShop,
                                Objects.requireNonNull(tab))
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
