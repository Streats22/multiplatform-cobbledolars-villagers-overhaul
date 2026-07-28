package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards zero-CD buy costA shrinking: free-minimum emerald trades must not delete emeralds.
 */
class FreeMinimumCostAPolicyTest {

    @Test
    void freeMinimumEmeraldDoesNotShrinkCostA() {
        // costA is emerald (CD-priced / free-minimum) → never item-shrink
        assertFalse(CobbleDollarsShopPayloadHandlers.shouldShrinkCostAWhenTotalCostZero(false, true));
    }

    @Test
    void emptyCostADoesNotShrink() {
        assertFalse(CobbleDollarsShopPayloadHandlers.shouldShrinkCostAWhenTotalCostZero(true, false));
    }

    @Test
    void trueItemBarterStillShrinksCostA() {
        assertTrue(CobbleDollarsShopPayloadHandlers.shouldShrinkCostAWhenTotalCostZero(false, false));
    }
}
