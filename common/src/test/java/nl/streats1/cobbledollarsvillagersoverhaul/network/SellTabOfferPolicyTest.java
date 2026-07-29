package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards Sell-tab index alignment: buy-classified hybrids must not appear in the sell list.
 */
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
        // emerald → relic_coin (or emerald → gold): Buy tab only
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                true, false, false, false, true));
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                true, false, false, true, false));
    }

    @Test
    void customCurrencyCostHybridIsBuyNotSell() {
        // relic_coin → emerald: Buy tab only; must not shift later Sell indices
        assertFalse(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, true, true, false, false));
    }

    @Test
    void goldConfiguredAsCurrencyStillSellsWhenCostIsPlainItem() {
        assertTrue(CobbleDollarsShopPayloadHandlers.isSellTabOffer(
                false, false, false, true, true));
    }
}
