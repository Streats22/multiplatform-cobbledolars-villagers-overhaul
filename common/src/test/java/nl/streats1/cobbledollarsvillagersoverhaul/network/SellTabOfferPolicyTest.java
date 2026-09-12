package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellTabOfferPolicyTest {

    @Test
    void normalItemForEmeraldIsSell() {
        assertTrue(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, false, true, false, false));
    }

    @Test
    void itemForGoldIngotIsSell() {
        assertTrue(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, false, false, true, false));
    }

    @Test
    void itemForCustomCurrencyIsSell() {
        assertTrue(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, false, false, false, true));
    }

    @Test
    void emeraldCostHybridIsBuyNotSell() {
        
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                true, false, false, false, true));
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                true, false, false, true, false));
    }

    @Test
    void customCurrencyCostHybridIsBuyNotSell() {
        
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, true, true, false, false));
    }

    @Test
    void goldConfiguredAsCurrencyStillSellsWhenCostIsPlainItem() {
        assertTrue(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, false, false, true, true));
    }
}
