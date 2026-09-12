package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantOfferDedupeTest {

    @Test
    void countsLaterDuplicateSignatures() {
        List<String> sigs = List.of(
                "minecraft:bread#6",
                "minecraft:wheat#20",
                "minecraft:bread#6",
                "minecraft:bread#6",
                "minecraft:pumpkin#1",
                "minecraft:bread#6");
        assertEquals(3, MerchantOfferDedupe.countRemovableDuplicateSignatures(sigs));
    }

    @Test
    void noDuplicatesWhenAllUnique() {
        List<String> sigs = List.of("a", "b", "c");
        assertEquals(0, MerchantOfferDedupe.countRemovableDuplicateSignatures(sigs));
    }

    @Test
    void emptyAndSingleAreZero() {
        assertEquals(0, MerchantOfferDedupe.countRemovableDuplicateSignatures(null));
        assertEquals(0, MerchantOfferDedupe.countRemovableDuplicateSignatures(List.of()));
        assertEquals(0, MerchantOfferDedupe.countRemovableDuplicateSignatures(List.of("only")));
    }

    @Test
    void mutableListStillCountsCorrectly() {
        List<String> sigs = new ArrayList<>();
        sigs.add("bread");
        sigs.add("bread");
        assertEquals(1, MerchantOfferDedupe.countRemovableDuplicateSignatures(sigs));
    }
}
