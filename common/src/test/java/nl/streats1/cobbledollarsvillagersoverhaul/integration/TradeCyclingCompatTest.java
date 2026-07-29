package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards trade-cycle rollback: failed regeneration must restore the pre-cycle offer snapshot.
 */
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
