package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DatapackItemPricing;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ShopOfferEntryFactory;
import nl.streats1.cobbledollarsvillagersoverhaul.util.TradeIngredientHelper;

import java.util.ArrayList;
import java.util.List;

public final class ShopOfferAssembler {

    private ShopOfferAssembler() {
    }

    public static List<MerchantOffer> buyOffers(List<MerchantOffer> allOffers) {
        List<MerchantOffer> buyOffers = new ArrayList<>();
        for (MerchantOffer o : allOffers) {
            if (o == null) {
                continue;
            }
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (costA == null || result == null || result.isEmpty()) {
                continue;
            }
            if (!costA.isEmpty() && (costA.is(Items.EMERALD)
                    || CustomCurrencyConfig.getCurrencyValue(costA) > 0)) {
                buyOffers.add(o);
            } else if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty()) {
                buyOffers.add(o);
            }
        }
        if (Config.USE_DATAPACK_TRADES) {
            for (MerchantOffer o : allOffers) {
                if (o == null) {
                    continue;
                }
                ItemStack costA = o.getCostA();
                ItemStack result = o.getResult();
                if (costA == null || result == null || costA.isEmpty() || result.isEmpty()) {
                    continue;
                }
                if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) {
                    continue;
                }
                if (CustomCurrencyConfig.isCurrencyItem(costA)) {
                    continue;
                }
                if (CustomCurrencyConfig.isCurrencyItem(result)) {
                    continue;
                }
                if (result.is(Items.GOLD_INGOT)) {
                    continue;
                }
                if (DatapackItemPricing.getOverridePrice(costA) > 0) {
                    buyOffers.add(o);
                }
            }
        }
        return buyOffers;
    }

    public static List<MerchantOffer> sellOffers(List<MerchantOffer> allOffers) {
        List<MerchantOffer> sellOffers = new ArrayList<>();
        for (MerchantOffer o : allOffers) {
            if (o == null) {
                continue;
            }
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (costA == null || result == null || costA.isEmpty() || result.isEmpty()) {
                continue;
            }
            if (!ShopTradePolicy.isSellTabOffer(
                    costA.is(Items.EMERALD),
                    CustomCurrencyConfig.getCurrencyValue(costA) > 0,
                    result.is(Items.EMERALD),
                    result.is(Items.GOLD_INGOT),
                    CustomCurrencyConfig.getCurrencyValue(result) > 0)) {
                continue;
            }
            sellOffers.add(o);
        }
        return sellOffers;
    }

    public static List<MerchantOffer> itemForItemOffers(List<MerchantOffer> allOffers) {
        List<MerchantOffer> tradeOffers = new ArrayList<>();
        for (MerchantOffer o : allOffers) {
            if (o == null) {
                continue;
            }
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (costA == null || result == null || costA.isEmpty() || result.isEmpty()) {
                continue;
            }
            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) {
                continue;
            }
            if (result.is(Items.GOLD_INGOT)) {
                continue;
            }
            if (CustomCurrencyConfig.isCurrencyItem(result)) {
                continue;
            }
            if (CustomCurrencyConfig.isCurrencyItem(costA)) {
                continue;
            }
            if (Config.USE_DATAPACK_TRADES && DatapackItemPricing.getOverridePrice(costA) > 0) {
                continue;
            }
            tradeOffers.add(o);
        }
        return tradeOffers;
    }

    public static void buildItemForItemTrades(List<MerchantOffer> allOffers,
                                             List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        for (MerchantOffer o : itemForItemOffers(allOffers)) {
            ItemStack merchantResult = o.getResult().copy();
            ItemStack merchantCostA = o.getCostA().copy();
            ItemStack merchantCostB = TradeIngredientHelper.secondaryIngredient(o);
            if (merchantResult.isEmpty() || merchantCostA.isEmpty()) {
                continue;
            }
            tradesOut.add(ShopOfferEntryFactory.trade(merchantCostA, merchantResult, merchantCostB));
        }
    }

    public static void buildOfferLists(List<MerchantOffer> allOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) {
                continue;
            }
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) {
                continue;
            }
            if (result.isEmpty()) {
                continue;
            }

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, costA.getCount(), safeCostB));
                }
                continue;
            }

            if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty() && !result.isEmpty()) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, 0, safeCostB));
                }
                continue;
            }
            if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
                int cobbleDollarsPerTrade = costA.getCount() * CustomCurrencyConfig.getCurrencyValue(costA);
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buyDirect(safeResult, cobbleDollarsPerTrade, safeCostB));
                }
                continue;
            }

            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sell(safeCostA, result.getCount()));
                }
                continue;
            }

            if (result.is(Items.GOLD_INGOT) && !costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(result) == 0) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sellDirect(safeCostA, DatapackItemPricing.getPrice(result)));
                }
                continue;
            }
            if (!result.isEmpty() && CustomCurrencyConfig.getCurrencyValue(result) > 0 && !costA.isEmpty()) {
                int cobbleDollarsPerTrade = result.getCount() * CustomCurrencyConfig.getCurrencyValue(result);
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sellDirect(safeCostA, cobbleDollarsPerTrade));
                }
            }
        }
    }

    public static void buildDatapackOffers(List<MerchantOffer> allOffers,
                                          List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                          List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        if (!Config.USE_DATAPACK_TRADES) {
            return;
        }

        for (MerchantOffer o : allOffers) {
            if (o == null) {
                continue;
            }
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) {
                continue;
            }
            if (costA.isEmpty() || result.isEmpty()) {
                continue;
            }

            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) {
                continue;
            }
            if (CustomCurrencyConfig.isCurrencyItem(costA)) {
                continue;
            }
            if (CustomCurrencyConfig.isCurrencyItem(result)) {
                continue;
            }
            if (result.is(Items.GOLD_INGOT)) {
                continue;
            }

            int price = DatapackItemPricing.getOverridePrice(costA);

            if (price > 0) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                buyOut.add(ShopOfferEntryFactory.buyDirect(safeResult, price, safeCostB));
            }
        }
    }
}
