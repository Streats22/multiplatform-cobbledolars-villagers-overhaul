package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRequestGuardsTest {
    @Test
    void quantityMustStayWithinClientTradeCap() {
        assertFalse(TradeRequestGuards.isAllowedQuantity(0));
        assertTrue(TradeRequestGuards.isAllowedQuantity(1));
        assertTrue(TradeRequestGuards.isAllowedQuantity(TradeRequestGuards.MAX_QUANTITY));
        assertFalse(TradeRequestGuards.isAllowedQuantity(TradeRequestGuards.MAX_QUANTITY + 1));
        assertFalse(TradeRequestGuards.isAllowedQuantity(Integer.MAX_VALUE));
    }
}
