package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void sellTakesBlankBookNotTheWrittenOneInAnEarlierSlot() {
        assertTrue(ShopTradePolicy.itemPaymentRequiresExactComponents());
        // Hotbar holds a written book and quill; the blank one the UI counted is later.
        boolean[] exact = {false, true};
        int[] counts = {1, 1};
        assertArrayEquals(new int[]{0, 1}, ShopTradePolicy.takeExactComponentSlots(exact, counts, 1));
    }

    @Test
    void inexactStacksAloneCannotPay() {
        boolean[] exact = {false, false};
        int[] counts = {3, 2};
        assertArrayEquals(new int[]{0, 0}, ShopTradePolicy.takeExactComponentSlots(exact, counts, 1));
    }

    @Test
    void zeroCdCostDoesNotShrinkCdPricedCostA() {
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, true));
        assertTrue(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(false, false));
        assertFalse(ShopTradePolicy.shouldShrinkCostAWhenTotalCostZero(true, false));
    }
}
