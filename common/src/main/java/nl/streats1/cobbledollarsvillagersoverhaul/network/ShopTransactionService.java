package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.*;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import nl.streats1.cobbledollarsvillagersoverhaul.util.PlayerInventoryHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.util.TradeIngredientHelper;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;

public final class ShopTransactionService {

    private ShopTransactionService() {
    }

    private static void finishShopTradeSession(AbstractVillager tradingMerchant, Entity entity, ServerPlayer serverPlayer, int villagerId, boolean tradeCompleted) {
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            if (tradeCompleted) {
                sendBalanceUpdate(serverPlayer, villagerId);
            }
            return;
        }
        if (!tradeCompleted || entity == null) {
            return;
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    public static void handleSellFromBank(ServerPlayer serverPlayer, int offerIndex, int quantity) {
        if (!ShopInteractionGuard.isValidQuantity(quantity)) {
            return;
        }
        if (!ShopInteractionGuard.canAccessVirtualShop(serverPlayer)) {
            return;
        }
        List<CobbleDollarsShopPayloads.ShopOfferEntry> bankOffers = CobbleDollarsConfigHelper.getBankSellOffers();
        if (offerIndex < 0 || offerIndex >= bankOffers.size()) {
            return;
        }
        CobbleDollarsShopPayloads.ShopOfferEntry entry = bankOffers.get(offerIndex);
        
        ItemStack costA = entry.result();
        int pricePerUnit = entry.emeraldCount();
        var toAddOpt = ShopInteractionGuard.safeMultiplyLong(pricePerUnit, quantity);
        if (toAddOpt.isEmpty()) {
            return;
        }
        long toAdd = toAddOpt.getAsLong();

        int perTrade = costA.getCount();
        var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(perTrade, quantity);
        if (totalNeededOpt.isEmpty()) {
            return;
        }
        int totalNeeded = totalNeededOpt.getAsInt();
        if (!playerHasShopItem(serverPlayer, costA, totalNeeded)) {
            return;
        }
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
            return;
        }
        takeShopItem(serverPlayer, costA, totalNeeded);
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        sendBalanceUpdate(serverPlayer, VirtualShopIds.VIRTUAL_ID_BANK);
    }

    public static void handleBuyFromConfig(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        if (!ShopInteractionGuard.isValidQuantity(quantity)) {
            return;
        }
        if (VirtualShopIds.isVirtualShop(villagerId) && !ShopInteractionGuard.canAccessVirtualShop(serverPlayer)) {
            return;
        }
        List<CobbleDollarsShopPayloads.ShopOfferEntry> configOffers = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();

        if (offerIndex < 0 || offerIndex >= configOffers.size()) {
            return;
        }

        CobbleDollarsShopPayloads.ShopOfferEntry entry = configOffers.get(offerIndex);
        long cost;
        if (entry.directPrice()) {
            var costOpt = ShopInteractionGuard.safeMultiplyLong(entry.emeraldCount(), quantity);
            if (costOpt.isEmpty()) {
                return;
            }
            cost = costOpt.getAsLong();
        } else {
            var unitOpt = ShopInteractionGuard.safeMultiplyLong(entry.emeraldCount(), quantity);
            if (unitOpt.isEmpty()) {
                return;
            }
            var costOpt = ShopInteractionGuard.safeMultiplyLong(unitOpt.getAsLong(), CobbleDollarsConfigHelper.getEffectiveEmeraldRate());
            if (costOpt.isEmpty()) {
                return;
            }
            cost = costOpt.getAsLong();
        }

        long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);

        if (balanceBefore < cost) {
            return;
        }

        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) {
            return;
        }

        ItemStack out = entry.result().copy();
        if (!out.isEmpty() && !out.is(Items.AIR)) {
            var countOpt = ShopInteractionGuard.safeMultiplyExact(Math.max(1, out.getCount()), quantity);
            if (countOpt.isEmpty()) {
                CobbleDollarsIntegration.addBalance(serverPlayer, cost);
                return;
            }
            out.setCount(countOpt.getAsInt());
            PlayerInventoryHelper.give(serverPlayer, out);
        }

        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

        sendBalanceUpdate(serverPlayer, villagerId);
    }

    public static void handleBuy(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab, String selectedSeries) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            return;
        }
        if (!ShopInteractionGuard.isValidQuantity(quantity)) {
            return;
        }
        selectedSeries = ShopInteractionGuard.sanitizeSeriesId(selectedSeries);

        
        if (VirtualShopIds.isVirtualShop(villagerId)) {
            if (!ShopInteractionGuard.canAccessVirtualShop(serverPlayer)) {
                return;
            }
            handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
            return;
        }
        if (VirtualShopIds.isVirtual(villagerId)) {
            
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity))
            return;
        if (!ShopInteractionGuard.isWithinInteractRange(serverPlayer, entity)) {
            return;
        }
        if (ShopInteractionGuard.isExcludedProfession(entity)) {
            return;
        }

        boolean configBuyOffersAvailable = !CobbleDollarsConfigHelper.getDefaultShopBuyOffers().isEmpty();
        boolean assignedOrVirtualConfig = ShopInteractionGuard.isConfigShopBuy(villagerId, entity);
        if (ShopTradePolicy.shouldBuyFromConfigShop(assignedOrVirtualConfig, fromConfigShop, false)) {
            handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
            return;
        }

        if (configBuyOffersAvailable && entity instanceof Villager emptyCheckVillager) {
            if (McaIntegration.isVillager(emptyCheckVillager)) {
                McaIntegration.prepareOffers(level, emptyCheckVillager);
            } else {
                VillagerConfigCompat.prepareVillagerForShop(level, emptyCheckVillager);
            }
            if (ShopInteractionGuard.isEmptyOfferConfigFallback(emptyCheckVillager, true)) {
                handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
                return;
            }
        } else if (configBuyOffersAvailable && entity instanceof WanderingTrader emptyCheckTrader) {
            MerchantTradeGenerationHelper.ensureMerchantOffersReady(level, emptyCheckTrader);
            if (ShopInteractionGuard.isEmptyOfferConfigFallback(emptyCheckTrader, true)) {
                handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
                return;
            }
        }
        
        

        
        AbstractVillager tradingMerchant = null;
        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            v.setTradingPlayer(serverPlayer);
            ShopTradeSession.updateVillagerSpecialPrices(v, serverPlayer);
            tradingMerchant = v;
            if (McaIntegration.isVillager(v)) {
                McaIntegration.prepareOffers(level, v);
            }
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            trader.setTradingPlayer(serverPlayer);
            tradingMerchant = trader;
            allOffers = trader.getOffers();
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
            } catch (Exception e) {
            }

            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                } else {
                    allOffers = List.of();
                }
            } catch (Exception e) {
                allOffers = List.of();
            }
        } else {
            return;
        }

        boolean completedTrade = false;
        try {
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            List<MerchantOffer> filteredOffers;
            if (tab == 0) {
                var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
                filteredOffers = allOffers.stream()
                        .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                        .toList();
            } else if (tab == 2) {
                filteredOffers = allOffers.stream()
                        .filter(o -> !o.getCostA().isEmpty() && ShopSeriesCatalog.isTrainerCard(o.getCostA().getItem()))
                        .toList();
            } else {
                filteredOffers = allOffers;
            }

            if (offerIndex < 0 || offerIndex >= filteredOffers.size()) return;
            offer = filteredOffers.get(offerIndex);
        } else if (tab == 2) {
            List<MerchantOffer> tradeOffersList = ShopOfferAssembler.itemForItemOffers(allOffers);
            if (offerIndex < 0 || offerIndex >= tradeOffersList.size()) return;
            offer = tradeOffersList.get(offerIndex);
        } else {
            List<MerchantOffer> buyOffersList = ShopOfferAssembler.buyOffers(allOffers);
            if (offerIndex < 0 || offerIndex >= buyOffersList.size()) return;
            offer = buyOffersList.get(offerIndex);
        }

        if (!ShopInteractionGuard.canFulfillQuantity(offer, quantity)) {
            return;
        }

        ItemStack costA = offer.getCostA();
        if (tab == 2 && RctTrainerAssociationCompat.isTrainerAssociation(entity) && !costA.isEmpty() && ShopSeriesCatalog.isTrainerCard(costA.getItem())) {
            var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(costA.getCount(), quantity);
            if (totalNeededOpt.isEmpty()) {
                return;
            }
            int totalNeeded = totalNeededOpt.getAsInt();
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ShopSeriesCatalog.isTrainerCard(stack.getItem())) {
                    have += stack.getCount();
                }
            }
            if (have < totalNeeded) {
                return;
            }

            String targetSeries = selectedSeries;
            if (targetSeries == null || targetSeries.isEmpty()) {
                targetSeries = ShopSeriesCatalog.identifySeriesFromOffer(offer, serverPlayer, offerIndex);
            }
            if (targetSeries == null) {
                targetSeries = "";
            }
            List<String> availableIds = ShopSeriesCatalog.getLiveAvailableSeriesIds(serverPlayer);
            if (!ShopInteractionGuard.isSeriesAllowed(targetSeries, availableIds)) {
                return;
            }

            
            if (!targetSeries.isEmpty()) {
                boolean seriesSet = false;
                try {
                    var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                    var getInstanceMethod = rctModClass.getMethod("getInstance");
                    var rctModInstance = getInstanceMethod.invoke(null);

                    var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
                    var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
                    var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);

                    var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
                    var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
                    var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayer);

                    if (trainerPlayerData != null) {
                        var setCurrentSeriesMethod = trainerPlayerDataClass.getMethod("setCurrentSeries", String.class);
                        setCurrentSeriesMethod.invoke(trainerPlayerData, targetSeries);
                        seriesSet = true;
                    }
                } catch (Exception e) {
                }
                if (!seriesSet) {
                    return;
                }
            }

            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ShopSeriesCatalog.isTrainerCard(stack.getItem())) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }

            ShopSeriesCatalog.clearCache(serverPlayer.getUUID());

            ItemStack resultCopy = offer.getResult().copy();
            var resultCountOpt = ShopInteractionGuard.safeMultiplyExact(resultCopy.getCount(), quantity);
            if (resultCountOpt.isEmpty()) {
                return;
            }
            resultCopy.setCount(resultCountOpt.getAsInt());
            PlayerInventoryHelper.give(serverPlayer, resultCopy);

            Merchant merchant = null;
            if (entity instanceof Merchant) {
                merchant = (Merchant) entity;
            }

            ShopTradeSession.notifyTradeForQuantity(merchant, offer, quantity);

            ShopTradeSession.awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
            return;
        }

        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long totalCost;
        
        boolean costAIsCdPriced = false;

        if (costA.is(Items.EMERALD)) {
            costAIsCdPriced = true;
            var emeraldCostOpt = ShopInteractionGuard.safeMultiplyExact(costA.getCount(), quantity);
            if (emeraldCostOpt.isEmpty()) {
                return;
            }
            int emeraldCost = emeraldCostOpt.getAsInt();
            if (Config.FREE_MINIMUM_EMERALD_TRADE && emeraldCost == quantity && costA.getCount() == 1) {
                totalCost = 0;
            } else {
                var totalOpt = ShopInteractionGuard.safeMultiplyLong(emeraldCost, rate);
                if (totalOpt.isEmpty()) {
                    return;
                }
                totalCost = totalOpt.getAsLong();
            }
        } else if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
            costAIsCdPriced = true;
            var totalOpt = ShopInteractionGuard.safeMultiplyLong(CustomCurrencyConfig.getTotalValue(costA), quantity);
            if (totalOpt.isEmpty()) {
                return;
            }
            totalCost = totalOpt.getAsLong();
        } else if (!costA.isEmpty() && Config.USE_DATAPACK_TRADES && DatapackItemPricing.getOverridePrice(costA) > 0) {
            costAIsCdPriced = true;
            int pricePerTrade = DatapackItemPricing.getOverridePrice(costA);
            var totalOpt = ShopInteractionGuard.safeMultiplyLong(pricePerTrade, quantity);
            if (totalOpt.isEmpty()) {
                return;
            }
            totalCost = totalOpt.getAsLong();
        } else {
            var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(costA.getCount(), quantity);
            if (totalNeededOpt.isEmpty()) {
                return;
            }
            int totalNeeded = totalNeededOpt.getAsInt();
            if (!playerHasShopItem(serverPlayer, costA, totalNeeded)) {
                return;
            }
            totalCost = 0;
        }

        if (totalCost > 0) {
            long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);

            if (balanceBefore < totalCost) {
                return;
            }

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, -totalCost)) {
                return;
            }
        }

            java.util.Optional<net.minecraft.world.item.trading.ItemCost> itemCostB = offer.getItemCostB();
            if (itemCostB.isPresent()) {
                net.minecraft.world.item.trading.ItemCost cost = itemCostB.get();
                var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(cost.count(), quantity);
                if (totalNeededOpt.isEmpty()) {
                    if (totalCost > 0) {
                        CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                    }
                    return;
                }
                int totalNeeded = totalNeededOpt.getAsInt();
                if (!TradeIngredientHelper.hasInInventory(serverPlayer, cost, totalNeeded)) {
                    if (totalCost > 0) {
                        CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                    }
                    return;
                }
                TradeIngredientHelper.shrinkFromInventory(serverPlayer, cost, totalNeeded);
            } else {
                ItemStack costB = TradeIngredientHelper.secondaryIngredient(offer);
                if (!costB.isEmpty()) {
                    var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(costB.getCount(), quantity);
                    if (totalNeededOpt.isEmpty()) {
                        if (totalCost > 0) {
                            CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                        }
                        return;
                    }
                    int totalNeeded = totalNeededOpt.getAsInt();
                    if (!PlayerInventoryHelper.hasEnough(serverPlayer, costB, totalNeeded)) {
                        if (totalCost > 0) {
                            CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                        }
                        return;
                    }
                    PlayerInventoryHelper.shrink(serverPlayer, costB, totalNeeded);
                }
        }

        if (totalCost == 0 && ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(costA.isEmpty(), costAIsCdPriced)) {
            var shrinkOpt = ShopInteractionGuard.safeMultiplyExact(costA.getCount(), quantity);
            if (shrinkOpt.isEmpty()) {
                return;
            }
            takeShopItem(serverPlayer, costA, shrinkOpt.getAsInt());
        }

        ItemStack result = offer.getResult().copy();
        var resultCountOpt = ShopInteractionGuard.safeMultiplyExact(result.getCount(), quantity);
        if (resultCountOpt.isEmpty()) {
            if (totalCost > 0) {
                CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
            }
            return;
        }
        result.setCount(resultCountOpt.getAsInt());
        PlayerInventoryHelper.give(serverPlayer, result);

        Merchant merchant = null;
        if (entity instanceof Merchant) {
            merchant = (Merchant) entity;
        }

            ShopTradeSession.notifyTradeForQuantity(merchant, offer, quantity);

        ShopTradeSession.awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
        } finally {
            if (tradingMerchant != null && ShopTradePolicy.shouldReleaseTradingPlayerAfterShopTrade()) {
                tradingMerchant.setTradingPlayer(null);
            }
            finishShopTradeSession(tradingMerchant, entity, serverPlayer, villagerId, completedTrade);
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            return;
        }
        if (!ShopInteractionGuard.isValidQuantity(quantity)) {
            return;
        }

        
        if (VirtualShopIds.isVirtualBank(villagerId)) {
            handleSellFromBank(serverPlayer, offerIndex, quantity);
            return;
        }
        if (VirtualShopIds.isVirtual(villagerId)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);

        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            return;
        }
        if (!ShopInteractionGuard.isWithinInteractRange(serverPlayer, entity)) {
            return;
        }
        if (ShopInteractionGuard.isExcludedProfession(entity)) {
            return;
        }

        
        AbstractVillager tradingMerchant = null;
        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            v.setTradingPlayer(serverPlayer);
            ShopTradeSession.updateVillagerSpecialPrices(v, serverPlayer);
            tradingMerchant = v;
            if (McaIntegration.isVillager(v)) {
                McaIntegration.prepareOffers(level, v);
            }
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            trader.setTradingPlayer(serverPlayer);
            tradingMerchant = trader;
            allOffers = trader.getOffers();
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                } else {
                    allOffers = List.of();
                }
            } catch (Exception e) {
                allOffers = List.of();
            }
        } else {
            return;
        }

        boolean completedTrade = false;
        try {
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            if (offerIndex < 0 || offerIndex >= allOffers.size()) {
                return;
            }
            offer = allOffers.get(offerIndex);
        } else {
            List<MerchantOffer> sellOffers = ShopOfferAssembler.sellOffers(allOffers);
            if (offerIndex < 0 || offerIndex >= sellOffers.size()) {
                return;
            }
            offer = sellOffers.get(offerIndex);
        }

        if (!ShopInteractionGuard.canFulfillQuantity(offer, quantity)) {
            return;
        }

            ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();

            if (costA.isEmpty()) {
            return;
        }

        int perTrade = costA.getCount();
        var totalNeededOpt = ShopInteractionGuard.safeMultiplyExact(perTrade, quantity);
        if (totalNeededOpt.isEmpty()) {
            return;
        }
        int totalNeeded = totalNeededOpt.getAsInt();
            if (!playerHasShopItem(serverPlayer, costA, totalNeeded)) {
            return;
        }

        
        if (result.is(Items.EMERALD)) {
            var emeraldCountOpt = ShopInteractionGuard.safeMultiplyExact(result.getCount(), quantity);
            if (emeraldCountOpt.isEmpty()) {
                return;
            }
            int emeraldCount = emeraldCountOpt.getAsInt();
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            var toAddOpt = ShopInteractionGuard.safeMultiplyLong(emeraldCount, rate);
            if (toAddOpt.isEmpty()) {
                return;
            }
            long toAdd = toAddOpt.getAsLong();

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
            takeShopItem(serverPlayer, costA, totalNeeded);
        } else if (result.is(Items.GOLD_INGOT) && CustomCurrencyConfig.getCurrencyValue(result) == 0) {
            ItemStack resultForQty = result.copy();
            var countOpt = ShopInteractionGuard.safeMultiplyExact(result.getCount(), quantity);
            if (countOpt.isEmpty()) {
                return;
            }
            resultForQty.setCount(countOpt.getAsInt());
            long toAdd = DatapackItemPricing.getPrice(resultForQty);
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
            takeShopItem(serverPlayer, costA, totalNeeded);
        } else if (CustomCurrencyConfig.getCurrencyValue(result) > 0) {
            ItemStack resultForQty = result.copy();
            var countOpt = ShopInteractionGuard.safeMultiplyExact(result.getCount(), quantity);
            if (countOpt.isEmpty()) {
                return;
            }
            resultForQty.setCount(countOpt.getAsInt());
            long toAdd = CustomCurrencyConfig.getTotalValue(resultForQty);
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
            takeShopItem(serverPlayer, costA, totalNeeded);
        } else {
            ItemStack resultCopy = result.copy();
            var countOpt = ShopInteractionGuard.safeMultiplyExact(result.getCount(), quantity);
            if (countOpt.isEmpty()) {
                return;
            }
            resultCopy.setCount(countOpt.getAsInt());
            takeShopItem(serverPlayer, costA, totalNeeded);
            PlayerInventoryHelper.give(serverPlayer, resultCopy);
        }

        if (entity instanceof Merchant merchant) {
            ShopTradeSession.notifyTradeForQuantity(merchant, offer, quantity);
        }

        ShopTradeSession.awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
        } finally {
            if (tradingMerchant != null && ShopTradePolicy.shouldReleaseTradingPlayerAfterShopTrade()) {
                tradingMerchant.setTradingPlayer(null);
            }
            finishShopTradeSession(tradingMerchant, entity, serverPlayer, villagerId, completedTrade);
        }
    }

    /**
     * Same rule as the shop screen: only stacks with the same item and components count.
     * A same-item-only check would let a hotbar variant (written book, lodestone compass)
     * satisfy the trade and be removed before the plain stack the UI counted.
     */
    private static boolean playerHasShopItem(ServerPlayer player, ItemStack needle, int required) {
        return ShopTradePolicy.itemPaymentRequiresExactComponents()
                && PlayerInventoryHelper.hasEnoughExact(player, needle, required);
    }

    private static void takeShopItem(ServerPlayer player, ItemStack needle, int amount) {
        if (!ShopTradePolicy.itemPaymentRequiresExactComponents() || amount <= 0) {
            return;
        }
        PlayerInventoryHelper.shrinkExact(player, needle, amount);
    }

    private static void sendBalanceUpdate(ServerPlayer player, int villagerId) {
        long balance = CobbleDollarsIntegration.getBalance(player);
        if (balance < 0) balance = 0;
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.BalanceUpdate(villagerId, balance));
    }

}
