package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopInteractionGuardTest {

    @Test
    void quantityMustBeBetweenOneAndMax() {
        assertFalse(ShopInteractionGuard.isValidQuantity(0));
        assertFalse(ShopInteractionGuard.isValidQuantity(-1));
        assertFalse(ShopInteractionGuard.isValidQuantity(ShopInteractionGuard.MAX_TRADE_QUANTITY + 1));
        assertTrue(ShopInteractionGuard.isValidQuantity(1));
        assertTrue(ShopInteractionGuard.isValidQuantity(ShopInteractionGuard.MAX_TRADE_QUANTITY));
    }

    @Test
    void multiplyExactRejectsOverflowThatWouldEnableFreeBuys() {
        
        OptionalInt overflow = ShopInteractionGuard.safeMultiplyExact(64, 33_554_432);
        assertTrue(overflow.isEmpty());

        OptionalInt ok = ShopInteractionGuard.safeMultiplyExact(64, 64);
        assertTrue(ok.isPresent());
        assertEquals(4096, ok.getAsInt());
    }

    @Test
    void multiplyLongRejectsOverflowAndNegatives() {
        assertTrue(ShopInteractionGuard.safeMultiplyLong(-1, 2).isEmpty());
        assertTrue(ShopInteractionGuard.safeMultiplyLong(Long.MAX_VALUE, 2).isEmpty());
        OptionalLong ok = ShopInteractionGuard.safeMultiplyLong(750, 64);
        assertTrue(ok.isPresent());
        assertEquals(48_000L, ok.getAsLong());
    }

    @Test
    void seriesSanitizeTruncatesAndAllowsEmpty() {
        assertEquals("", ShopInteractionGuard.sanitizeSeriesId(null));
        assertEquals("", ShopInteractionGuard.sanitizeSeriesId(""));
        String longId = "a".repeat(ShopInteractionGuard.MAX_SERIES_ID_LENGTH + 40);
        assertEquals(ShopInteractionGuard.MAX_SERIES_ID_LENGTH, ShopInteractionGuard.sanitizeSeriesId(longId).length());
    }

    @Test
    void seriesAllowlistRejectsUnknownIds() {
        assertTrue(ShopInteractionGuard.isSeriesAllowed("", List.of("alpha")));
        assertTrue(ShopInteractionGuard.isSeriesAllowed("alpha", List.of("alpha", "beta")));
        assertFalse(ShopInteractionGuard.isSeriesAllowed("evil", List.of("alpha", "beta")));
        assertFalse(ShopInteractionGuard.isSeriesAllowed("alpha", null));
    }

    @Test
    void virtualIdsAreRecognizedSeparatelyFromInteractGuards() {
        assertTrue(nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds.isVirtualShop(-1));
        assertTrue(nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds.isVirtualBank(-2));
        assertFalse(nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds.isVirtual(42));
    }
}
