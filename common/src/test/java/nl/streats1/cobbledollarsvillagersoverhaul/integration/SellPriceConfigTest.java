package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellPriceConfigTest {

    @Test
    void lowPricesAreKeptNotRoundedToRateSteps() {
        Map<String, Integer> in = new LinkedHashMap<>();
        in.put("minecraft:wheat", 1);
        in.put("minecraft:carrot", 37);
        Map<String, Integer> out = SellPriceConfig.sanitize(in);
        assertEquals(1, out.get("minecraft:wheat"));
        assertEquals(37, out.get("minecraft:carrot"));
    }

    @Test
    void bareIdsGetMinecraftNamespaceAndInvalidEntriesAreDropped() {
        Map<String, Integer> in = new LinkedHashMap<>();
        in.put("Wheat", 5);
        in.put("bad id!", 5);
        in.put("minecraft:zero", 0);
        in.put("minecraft:neg", -4);
        Map<String, Integer> out = SellPriceConfig.sanitize(in);
        assertEquals(5, out.get("minecraft:wheat"));
        assertEquals(1, out.size());
        assertFalse(out.containsKey("minecraft:zero"));
    }

    @Test
    void pricesAreCappedAtMax() {
        Map<String, Integer> out = SellPriceConfig.sanitize(Map.of("minecraft:diamond", Integer.MAX_VALUE));
        assertEquals(SellPriceConfig.MAX_PRICE, out.get("minecraft:diamond"));
        assertNull(SellPriceConfig.sanitize(null).get("x"));
    }
}
