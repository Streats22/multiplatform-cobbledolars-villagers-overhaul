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
    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL = (StreamCodec) ByteBufCodecs.BOOL;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_UTF8 = (StreamCodec) ByteBufCodecs.STRING_UTF8;
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = ItemStack.STREAM_CODEC;

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

    private static List<String> decodeNames(RegistryFriendlyByteBuf buf) {
        int n = VAR_INT.decode(buf);
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(STRING_UTF8.decode(buf));
        return out;
    }

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

    public record ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig, boolean canCycleTrades, String labelsJson, List<String> categoryNames, int hiddenTabs) implements CustomPacketPayload {
    public static final int HIDE_SELL_TAB = 1;
    public static final int HIDE_TRADES_TAB = 2;

    /** Shop with category names but nothing hidden. */
    public ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig, boolean canCycleTrades, String labelsJson, List<String> categoryNames) {
        this(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades, labelsJson, categoryNames, 0);
    }

    /** Shop with per-villager tab names but no extra category names. */
    public ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig, boolean canCycleTrades, String labelsJson) {
        this(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades, labelsJson, List.of(), 0);
    }

    /** Shop without per-villager tab names. */
    public ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, List<ShopOfferEntry> tradesOffers, boolean buyOffersFromConfig, boolean canCycleTrades) {
        this(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades, "", List.of(), 0);
    }

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
                            STRING_UTF8.encode(buf, data.labelsJson() != null ? data.labelsJson() : "");
                            List<String> names = data.categoryNames() != null ? data.categoryNames() : List.of();
                            VAR_INT.encode(buf, names.size());
                            for (String name : names) STRING_UTF8.encode(buf, name);
                            VAR_INT.encode(buf, data.hiddenTabs());
                        },
                        buf -> new ShopData(
                                VAR_INT.decode(buf),
                                VAR_LONG.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                OFFERS_LIST_CODEC.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                STRING_UTF8.decode(buf),
                                decodeNames(buf),
                                VAR_INT.decode(buf))
                );

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerShopConfigSync(
            boolean useCobbleDollarsShopUi,
            boolean villagersAcceptCobbleDollars,
            boolean useDatapackTrades,
            boolean useRctTradesOverhaul,
            int emeraldRateCdPerEmerald,
            boolean syncCobbleDollarsBankRate,
            String shopTitle,
            String buyTab,
            String sellTab,
            String tradesTab
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
                            VAR_INT.encode(buf, data.emeraldRateCdPerEmerald());
                            BOOL.encode(buf, data.syncCobbleDollarsBankRate());
                            STRING_UTF8.encode(buf, data.shopTitle() != null ? data.shopTitle() : "");
                            STRING_UTF8.encode(buf, data.buyTab() != null ? data.buyTab() : "");
                            STRING_UTF8.encode(buf, data.sellTab() != null ? data.sellTab() : "");
                            STRING_UTF8.encode(buf, data.tradesTab() != null ? data.tradesTab() : "");
                        },
                        buf -> new ServerShopConfigSync(
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                BOOL.decode(buf),
                                VAR_INT.decode(buf),
                                BOOL.decode(buf),
                                STRING_UTF8.decode(buf),
                                STRING_UTF8.decode(buf),
                                STRING_UTF8.decode(buf),
                                STRING_UTF8.decode(buf))
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

    public record RequestAssignedList(boolean unused) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestAssignedList> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("request_assigned_list")));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAssignedList> STREAM_CODEC =
                StreamCodec.composite(BOOL, RequestAssignedList::unused, RequestAssignedList::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AssignedList(List<String> uuids, List<String> labels) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AssignedList> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("assigned_list")));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignedList> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            int n = Math.min(data.uuids().size(), data.labels().size());
                            VAR_INT.encode(buf, n);
                            for (int i = 0; i < n; i++) {
                                STRING_UTF8.encode(buf, data.uuids().get(i));
                                STRING_UTF8.encode(buf, data.labels().get(i));
                            }
                        },
                        buf -> {
                            int n = Math.min(VAR_INT.decode(buf), 512);
                            List<String> uuids = new ArrayList<>(n);
                            List<String> labels = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                uuids.add(STRING_UTF8.decode(buf));
                                labels.add(STRING_UTF8.decode(buf));
                            }
                            return new AssignedList(uuids, labels);
                        });

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UnassignByUuid(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnassignByUuid> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("unassign_by_uuid")));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnassignByUuid> STREAM_CODEC =
                StreamCodec.composite(STRING_UTF8, UnassignByUuid::uuid, UnassignByUuid::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestAdminConfig(String kind) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestAdminConfig> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("request_admin_config")));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAdminConfig> STREAM_CODEC =
                StreamCodec.composite(STRING_UTF8, RequestAdminConfig::kind, RequestAdminConfig::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AdminConfigData(String kind, String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AdminConfigData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("admin_config_data")));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdminConfigData> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            STRING_UTF8.encode(buf, data.kind());
                            buf.writeUtf(data.json(), 262144);
                        },
                        buf -> new AdminConfigData(STRING_UTF8.decode(buf), buf.readUtf(262144)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SaveAdminConfig(String kind, String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveAdminConfig> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("save_admin_config")));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveAdminConfig> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            STRING_UTF8.encode(buf, data.kind());
                            buf.writeUtf(data.json(), 262144);
                        },
                        buf -> new SaveAdminConfig(STRING_UTF8.decode(buf), buf.readUtf(262144)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

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
}
