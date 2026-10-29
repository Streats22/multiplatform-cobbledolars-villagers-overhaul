package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.AssignModeTracker;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.*;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import nl.streats1.cobbledollarsvillagersoverhaul.util.PlayerInventoryHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ShopOfferEntryFactory;
import nl.streats1.cobbledollarsvillagersoverhaul.util.TradeIngredientHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CobbleDollarsShopPayloadHandlers {




    public static void handleShopScreenClosed(ServerPlayer serverPlayer, int villagerId) {
        if (VirtualShopIds.isVirtual(villagerId)) {
            return;
        }
        Entity entity = serverPlayer.serverLevel().getEntity(villagerId);
        if (entity instanceof AbstractVillager v && shouldReleaseMerchantOnDisconnect(v.getTradingPlayer(), serverPlayer)) {
            v.setTradingPlayer(null);
        }
    }

    static boolean shouldReleaseMerchantOnDisconnect(Player tradingPlayer, ServerPlayer disconnected) {
        return ShopTradePolicy.shouldReleaseMerchantOnDisconnect(tradingPlayer, disconnected);
    }

    static boolean shouldShrinkCostAWhenTotalCostZero(boolean costAEmpty, boolean costAIsCdPriced) {
        return ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(costAEmpty, costAIsCdPriced);
    }

    public static void handlePlayerDisconnect(ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return;
        }
        ConfigShopBuySession.forgetPlayer(serverPlayer.getUUID());
        AssignModeTracker.clear(serverPlayer.getUUID());
        ShopSeriesCatalog.clearCache(serverPlayer.getUUID());
        var server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbstractVillager villager
                        && shouldReleaseMerchantOnDisconnect(villager.getTradingPlayer(), serverPlayer)) {
                    villager.setTradingPlayer(null);
                }
            }
        }
    }


    public static void registerPayloads() {
    }

    public static void sendServerShopConfigTo(ServerPlayer player) {
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.ServerShopConfigSync(
                Config.USE_COBBLEDOLLARS_SHOP_UI,
                Config.VILLAGERS_ACCEPT_COBBLEDOLLARS,
                Config.USE_DATAPACK_TRADES,
                Config.USE_RCT_TRADES_OVERHAUL,
                nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper.getEffectiveEmeraldRate(),
                Config.SYNC_COBBLEDOLLARS_BANK_RATE));
    }

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        handleRequestShopData(serverPlayer, villagerId, 0);
    }

    private static void openVanillaMerchantMenu(ServerPlayer serverPlayer, int villagerId) {
        Entity entity = serverPlayer.serverLevel().getEntity(villagerId);
        if (entity == null) {
            return;
        }
        if (!ShopInteractionGuard.isWithinInteractRange(serverPlayer, entity)) {
            return;
        }
        if (entity instanceof MenuProvider menuProvider) {
            serverPlayer.openMenu(menuProvider);
        }
    }

    private static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId, int entityLookupRetry) {

        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }

        if (!ShopInteractionGuard.allowVirtualShopAccess(serverPlayer, villagerId)) {
            return;
        }

        sendServerShopConfigTo(serverPlayer);

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = VirtualShopIds.isVirtual(villagerId) ? null : level.getEntity(villagerId);

        if (entity == null) {
            if (VirtualShopIds.isVirtualShop(villagerId)) {
                List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
                try {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, balance, configBuy, List.of(), List.of(), true, false));
                } catch (Exception e) {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
                }
                return;
            }
            if (VirtualShopIds.isVirtualBank(villagerId)) {
                List<CobbleDollarsShopPayloads.ShopOfferEntry> bankSell = CobbleDollarsConfigHelper.getBankSellOffers();
                try {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, balance, List.of(), bankSell, List.of(), false, false));
                } catch (Exception e) {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
                }
                return;
            }
            return;
        }

        if (!ShopInteractionGuard.isWithinInteractRange(serverPlayer, entity)) {
            return;
        }

        if (ShopInteractionGuard.isExcludedProfession(entity)) {
            ResourceLocation profId = entity instanceof Villager v
                    ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession())
                    : null;
            if (entity instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider);
            }
            return;
        }

        List<MerchantOffer> allOffers = null;

        if (entity instanceof Villager villager) {
            villager.setTradingPlayer(serverPlayer);
            ShopTradeSession.updateVillagerSpecialPrices(villager, serverPlayer);
            try {
                if (McaIntegration.isVillager(villager)) {
                    McaIntegration.prepareOffers(serverPlayer.serverLevel(), villager);
                } else {
                    VillagerConfigCompat.prepareVillagerForShop(serverPlayer.serverLevel(), villager);
                }
                if (VillagerShopConfig.usesConfigShop(villager.getUUID())) {
                    List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
                    if (!configBuy.isEmpty()) {
                        buyOffers.addAll(configBuy);
                        buyOffersFromConfig = true;
                    }
                } else {
                    MerchantOfferDedupe.removeIdenticalDuplicates(villager.getOffers());
                    allOffers = villager.getOffers();
                    buildOfferLists(allOffers, buyOffers, sellOffers);
                    if (Config.USE_DATAPACK_TRADES) {
                        buildDatapackOffers(allOffers, buyOffers, sellOffers);
                    }
                    buildItemForItemTrades(allOffers, tradesOffers);
                }
            } finally {
                villager.setTradingPlayer(null);
            }
        } else if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                for (var method : entity.getClass().getDeclaredMethods()) {
                    if (method.getName().equals("updateTrades") || method.getName().startsWith("method_")) {
                        try {
                            method.setAccessible(true);

                            var itemOffersField = entity.getClass().getDeclaredField("itemOffers");
                            itemOffersField.setAccessible(true);
                            var before = itemOffersField.get(entity);

                            method.invoke(entity);

                            var after = itemOffersField.get(entity);

                            if (before == null && after != null || (before != null && after != null &&
                                    ((net.minecraft.world.item.trading.MerchantOffers) before).size() <
                                            ((net.minecraft.world.item.trading.MerchantOffers) after).size())) {
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
            }

            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
            } catch (Exception e) {
            }

            List<MerchantOffer> rctaOffers = new ArrayList<>();

            try {
                var merchantOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                if (merchantOffers instanceof List) {
                    for (var offer : merchantOffers) {
                        if (offer != null && !rctaOffers.contains(offer)) {
                            rctaOffers.add(offer);
                        }
                    }
                }
            } catch (Exception e) {
            }

            if (!rctaOffers.isEmpty()) {
                buildRctaOfferLists(rctaOffers, buyOffers, sellOffers, tradesOffers, serverPlayer);
            } else {
                var fallbackOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                buildOfferLists(fallbackOffers, buyOffers, sellOffers);
            }
        } else if (entity instanceof WanderingTrader trader) {
            MerchantTradeGenerationHelper.ensureMerchantOffersReady(serverPlayer.serverLevel(), trader);
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
            if (Config.USE_DATAPACK_TRADES) {
                buildDatapackOffers(allOffers, buyOffers, sellOffers);
            }
            buildItemForItemTrades(allOffers, tradesOffers);
        } else {
            return;
        }

        if (buyOffers.isEmpty() && sellOffers.isEmpty() && tradesOffers.isEmpty() && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
            if (!configBuy.isEmpty()) {
                buyOffers.addAll(configBuy);
                buyOffersFromConfig = true;
            }
        }
        boolean canCycleTrades = false;
        if (entity instanceof Villager villager && !buyOffersFromConfig
                && TradeCyclingModCompat.isTradeCyclingModLoaded()) {
            canCycleTrades = TradeCyclingCompat.canCycleTrades(villager);
        }

        
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeBuyOffers = buyOffers != null ? buyOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeSellOffers = sellOffers != null ? sellOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeTradesOffers = tradesOffers != null ? tradesOffers : List.of();

        try {
            ConfigShopBuySession.remember(serverPlayer.getUUID(), entity.getUUID(), buyOffersFromConfig);
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, safeBuyOffers, safeSellOffers, safeTradesOffers, buyOffersFromConfig, canCycleTrades));
        } catch (Exception e) {
            ConfigShopBuySession.remember(serverPlayer.getUUID(), entity.getUUID(), false);
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
        }

    }

    public static void handleCycleTrades(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) return;
        if (!ShopInteractionGuard.isWithinInteractRange(serverPlayer, entity)) return;
        if (ShopInteractionGuard.isExcludedProfession(entity)) return;
        if (!TradeCyclingCompat.canCycleTrades(villager)) return;
        TradeCyclingCompat.cycleTrades(villager, serverPlayer, () -> handleRequestShopData(serverPlayer, villagerId));
    }

    public static void handleAssignVillager(ServerPlayer serverPlayer, int villagerId) {
        if (!serverPlayer.hasPermissions(2)) return;
        if (!AssignModeTracker.isInAnyMode(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.not_in_mode"));
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.not_villager"));
            return;
        }
        if (villager.distanceTo(serverPlayer) > 6) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.too_far"));
            return;
        }
        if (AssignModeTracker.isInAssignMode(serverPlayer.getUUID())) {
            VillagerShopConfig.add(villager.getUUID());
            AssignModeTracker.clear(serverPlayer.getUUID());
            PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(false));
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.success"));
        } else {
            VillagerShopConfig.remove(villager.getUUID());
            AssignModeTracker.clear(serverPlayer.getUUID());
            PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(false));
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.unassign.success"));
        }
    }

    public static void handleBuy(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab, String selectedSeries) {
        // fromConfigShop is client-written. Catalog selection uses ConfigShopBuySession.
        ShopTransactionService.handleBuy(serverPlayer, villagerId, offerIndex, quantity, tab, selectedSeries);
    }

    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        ShopTransactionService.handleSell(serverPlayer, villagerId, offerIndex, quantity);
    }



    private static void buildRctaOfferLists(List<MerchantOffer> allOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut,
                                            ServerPlayer serverPlayer) {
        List<ShopSeriesCatalog.SeriesDisplay> availableSeries = ShopSeriesCatalog.getPlayerAvailableSeries(serverPlayer);

        int tradeIndex = 0;

        for (MerchantOffer o : allOffers) {
            if (o == null) continue;

            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (result.isEmpty()) continue;

            boolean isSeriesTrade = ShopSeriesCatalog.isTrainerCard(costA.getItem()) && ShopSeriesCatalog.isTrainerCard(result.getItem());
            String seriesId = "";
            String seriesName = "";
            String seriesTooltip = "";
            int seriesDifficulty = 5;
            int seriesCompleted = 0;

            if (isSeriesTrade) {
                if (tradeIndex < availableSeries.size()) {
                    ShopSeriesCatalog.SeriesDisplay info = availableSeries.get(tradeIndex);
                    seriesId = info.id();
                    seriesName = info.title();
                    seriesTooltip = info.tooltip();
                    seriesDifficulty = info.difficulty();
                    seriesCompleted = info.completed();
                } else {
                    seriesName = "Unknown Series";
                }
                tradeIndex++;
            }

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, costA.getCount(), safeCostB));
                }
            } else if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty() && !result.isEmpty()) {
                
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, 0, safeCostB));
                }
            } else if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sell(safeCostA, result.getCount()));
                }
            } else if (!costA.isEmpty() && !result.isEmpty() &&
                    !costA.is(Items.EMERALD) && !result.is(Items.EMERALD)) {
                ItemStack merchantResult = result.copy();
                ItemStack merchantCostA = costA.copy();
                ItemStack merchantCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!merchantResult.isEmpty() && !merchantCostA.isEmpty()) {
                    tradesOut.add(ShopOfferEntryFactory.seriesTrade(
                            merchantCostA, merchantResult, merchantCostB,
                            seriesId, seriesName, seriesTooltip, seriesDifficulty, seriesCompleted));
                }
            }
        }
    }

    private static List<MerchantOffer> getBuyOffersForVillager(List<MerchantOffer> allOffers) {
        return ShopOfferAssembler.buyOffers(allOffers);
    }

    private static List<MerchantOffer> getSellOffersForVillager(List<MerchantOffer> allOffers) {
        return ShopOfferAssembler.sellOffers(allOffers);
    }

    static boolean isSellTabOffer(boolean costAEmerald, boolean costACurrency,
                                  boolean resultEmerald, boolean resultGoldIngot, boolean resultCurrency) {
        return ShopTradePolicy.isSellTabOffer(costAEmerald, costACurrency, resultEmerald, resultGoldIngot, resultCurrency);
    }

    private static List<MerchantOffer> getItemForItemTradesForVillager(List<MerchantOffer> allOffers) {
        return ShopOfferAssembler.itemForItemOffers(allOffers);
    }

    private static void buildItemForItemTrades(List<MerchantOffer> allOffers,
                                               List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        ShopOfferAssembler.buildItemForItemTrades(allOffers, tradesOut);
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        ShopOfferAssembler.buildOfferLists(allOffers, buyOut, sellOut);
    }

    private static void buildDatapackOffers(List<MerchantOffer> allOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        ShopOfferAssembler.buildDatapackOffers(allOffers, buyOut, sellOut);
    }

    @SuppressWarnings("unused")
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                return findField(parent, fieldName);
            }
            return null;
        }
    }

    private static java.lang.reflect.Field findFieldByTypeName(Class<?> clazz, String typeName, Object instance) {
        var fields = clazz.getDeclaredFields();
        for (var field : fields) {
            if (field.getType().getSimpleName().equals(typeName)) {
                field.setAccessible(true);
                return field;
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && parent != Object.class) {
            return findFieldByTypeName(parent, typeName, instance);
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static void loadSeriesTrades(Object seriesManager, ServerPlayer serverPlayer,
                                         List<MerchantOffer> rctaOffers,
                                         List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var getSeriesMethod = seriesManager.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(seriesManager);

            if (series instanceof Iterable) {
                for (Object s : (Iterable<?>) series) {
                    var getOffersMethod = s.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(s);

                    if (offers instanceof List) {
                        for (Object offer : (List<?>) offers) {
                            if (offer instanceof MerchantOffer merchantOffer) {
                                if (!rctaOffers.contains(merchantOffer)) {
                                    rctaOffers.add(merchantOffer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                var getTradesMethod = seriesManager.getClass().getMethod("getTrades");
                var trades = getTradesMethod.invoke(seriesManager);
                if (trades instanceof List) {
                    for (Object trade : (List<?>) trades) {
                        if (trade instanceof MerchantOffer merchantOffer) {
                            if (!rctaOffers.contains(merchantOffer)) {
                                rctaOffers.add(merchantOffer);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @SuppressWarnings("unused")
    private static void loadFromTrainerPlayerData(Entity entity, ServerPlayer serverPlayer,
                                                  List<MerchantOffer> rctaOffers,
                                                  List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var trainerPlayerDataField = findFieldByTypeName(entity.getClass(), "TrainerPlayerData", entity);
            if (trainerPlayerDataField != null) {
                Object trainerPlayerData = trainerPlayerDataField.get(entity);
                if (trainerPlayerData != null) {
                    loadFromTrainerPlayerDataType(trainerPlayerData, serverPlayer, rctaOffers, tradesOut);
                }
            }
        } catch (Exception ex) {
        }
    }

    private static void loadFromTrainerPlayerDataType(Object trainerPlayerData, ServerPlayer serverPlayer,
                                                      List<MerchantOffer> rctaOffers,
                                                      List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var getSeriesMethod = trainerPlayerData.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(trainerPlayerData);
            if (series instanceof Iterable) {
                for (Object s : (Iterable<?>) series) {
                    var getOffersMethod = s.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(s);
                    if (offers instanceof List) {
                        for (Object offer : (List<?>) offers) {
                            if (offer instanceof MerchantOffer merchantOffer && !rctaOffers.contains(merchantOffer)) {
                                rctaOffers.add(merchantOffer);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    @SuppressWarnings("unused")
    private static void loadFromTrainerSpawn(Object spawn, ServerPlayer serverPlayer,
                                             List<MerchantOffer> rctaOffers,
                                             List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            try {
                var getPlayerMethod = spawn.getClass().getMethod("getPlayer");
                var player = getPlayerMethod.invoke(spawn);
                if (player != null && player.toString().contains(serverPlayer.getStringUUID())) {
                    loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
                }
            } catch (NoSuchMethodException e) {
                loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
            }
        } catch (Exception ex) {
        }
    }

}
