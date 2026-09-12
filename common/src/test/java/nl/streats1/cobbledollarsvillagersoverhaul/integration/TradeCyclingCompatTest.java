package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCyclingCompatTest {

    @Test
    void restoresWhenRegenerationProducesNoOffers() {
        assertTrue(TradeCyclingCompat.shouldRestoreOffersAfterCycle(false));
    }

    @Test
    void keepsNewOffersWhenRegenerationSucceeds() {
        assertFalse(TradeCyclingCompat.shouldRestoreOffersAfterCycle(true));
    }
}
