package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMinimumCostAPolicyTest {

    @Test
    void freeMinimumEmeraldDoesNotShrinkCostA() {
        
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
