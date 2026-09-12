package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaMerchantCompatTest {

    @Test
    void generatesOnlyWhenOffersMissingOrEmpty() {
        assertTrue(McaMerchantCompat.shouldGenerateMissingTrades(true));
    }

    @Test
    void doesNotRegenerateWhenOffersAlreadyPresent() {
        assertFalse(McaMerchantCompat.shouldGenerateMissingTrades(false));
    }
}
