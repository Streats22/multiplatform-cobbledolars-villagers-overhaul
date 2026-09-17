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
    void configBuyHonorsCatalogShownAtOpenAfterVillagerGainsOffers() {
        // Unemployed shop open → config UI; villager claims a job before Buy.
        assertTrue(ShopTradePolicy.shouldBuyFromConfigShop(false, true, false));
        assertTrue(ShopTradePolicy.shouldBuyFromConfigShop(true, false, false));
        assertTrue(ShopTradePolicy.shouldBuyFromConfigShop(false, false, true));
        assertFalse(ShopTradePolicy.shouldBuyFromConfigShop(false, false, false));
    }

    @Test
    void zeroCdCostDoesNotShrinkCdPricedCostA() {
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, true));
        assertTrue(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, false));
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(true, false));
    }
}
