package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;

/**
 * Universal handler for processing villager trades with CobbleDollars integration
 */
public final class UniversalTradeHandler {
    
    public enum TradeType {
        BUY_EMERALDS,    // Player gives items, gets emeralds
        SELL_EMERALDS,   // Player gives emeralds, gets items
        ITEM_FOR_ITEM    // Player gives items, gets items
    }
    
    private UniversalTradeHandler() {
        // Utility class
    }
    
    /**
     * Process a trade with CobbleDollars integration
     * Returns true if the trade was successfully processed
     */
    public static boolean processTrade(ServerPlayer player, MerchantOffer offer) {
        if (!CobbleDollarsIntegration.isAvailable()) {
            return false;
        }
        
        TradeType tradeType = classifyTrade(offer);
        
        switch (tradeType) {
            case BUY_EMERALDS:
                return handleBuyEmeralds(player, offer);
            case SELL_EMERALDS:
                return handleSellEmeralds(player, offer);
            case ITEM_FOR_ITEM:
                return handleItemForItem(player, offer);
            default:
                return false;
        }
    }
    
    /**
     * Classify the type of trade
     */
    public static TradeType classifyTrade(MerchantOffer offer) {
        ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();
        
        if (costA == null || result == null) {
            return TradeType.ITEM_FOR_ITEM;
        }
        
        if (result.is(Items.EMERALD)) {
            return TradeType.BUY_EMERALDS;
        } else if (costA.is(Items.EMERALD)) {
            return TradeType.SELL_EMERALDS;
        } else {
            return TradeType.ITEM_FOR_ITEM;
        }
    }
    
    private static boolean handleBuyEmeralds(ServerPlayer player, MerchantOffer offer) {
        // Player sells items to get emeralds
        // Give player CobbleDollars equivalent of the emeralds they would receive
        ItemStack result = offer.getResult();
        if (result.is(Items.EMERALD)) {
            int emeraldCount = result.getCount();
            long cobbleDollarsAmount = (long) emeraldCount * Config.COBBLEDOLLARS_EMERALD_RATE;
            
            return CobbleDollarsIntegration.addBalance(player, cobbleDollarsAmount);
        }
        return false;
    }
    
    private static boolean handleSellEmeralds(ServerPlayer player, MerchantOffer offer) {
        // Player buys items with emeralds
        // Check if player has enough CobbleDollars and deduct them
        ItemStack costA = offer.getCostA();
        if (costA.is(Items.EMERALD)) {
            int emeraldCount = costA.getCount();
            long cobbleDollarsAmount = (long) emeraldCount * Config.COBBLEDOLLARS_EMERALD_RATE;
            
            long currentBalance = CobbleDollarsIntegration.getBalance(player);
            if (currentBalance >= cobbleDollarsAmount) {
                return CobbleDollarsIntegration.addBalance(player, -cobbleDollarsAmount);
            }
        }
        return false;
    }
    
    private static boolean handleItemForItem(ServerPlayer player, MerchantOffer offer) {
        // For item-for-item trades, we can optionally give CobbleDollars as a bonus
        if (!Config.EARN_MONEY_FROM_NON_EMERALD_TRADES) {
            return true; // Allow the trade but don't process CobbleDollars
        }
        
        // Calculate value based on items involved and give bonus CobbleDollars
        ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();
        
        if (costA != null && result != null && !costA.isEmpty() && !result.isEmpty()) {
            // Simple valuation: give 50% of the emerald equivalent as bonus
            int estimatedValue = Math.max(costA.getCount(), result.getCount());
            long bonusAmount = (long) (estimatedValue * Config.COBBLEDOLLARS_EMERALD_RATE * Config.NON_EMERALD_TRADE_EARN_RATE);
            
            if (bonusAmount > 0) {
                return CobbleDollarsIntegration.addBalance(player, bonusAmount);
            }
        }
        
        return true;
    }
}
