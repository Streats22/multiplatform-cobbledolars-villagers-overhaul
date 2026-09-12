package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopTradePolicyTest {

    @Test
    void shopTradesAlwaysReleaseTradingPlayerSoVillagersCanRestock() {
        // Packet shop has no MerchantMenu; a stuck tradingPlayer blocks workstation restock.
        assertTrue(ShopTradePolicy.shouldReleaseTradingPlayerAfterShopTrade());
    }

    @Test
    void disconnectReleaseRequiresSamePlayerInstance() {
        assertFalse(ShopTradePolicy.shouldReleaseMerchantOnDisconnect(null, null));
    }

    @Test
    void zeroCdCostDoesNotShrinkCdPricedCostA() {
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, true));
        assertTrue(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, false));
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(true, false));
    }
}
