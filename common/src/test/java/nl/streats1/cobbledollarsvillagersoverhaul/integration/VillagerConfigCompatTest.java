package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards against reintroducing offer-list clears on shop open (infinite limited-stock restock).
 */
class VillagerConfigCompatTest {

    @Test
    void neverClearsOffersBeforeRefreshEvenWithCustomTradeTable() {
        assertFalse(VillagerConfigCompat.shouldClearOffersBeforeRefresh(true, 0));
        assertFalse(VillagerConfigCompat.shouldClearOffersBeforeRefresh(true, 4));
        assertFalse(VillagerConfigCompat.shouldClearOffersBeforeRefresh(false, 4));
    }
}
