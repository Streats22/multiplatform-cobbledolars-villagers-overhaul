package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaVillagerCompatTest {

    @Test
    void knownMcaVillagerPathsAreTradeable() {
        assertTrue(McaVillagerCompat.isKnownMcaVillagerEntityPath("male_villager"));
        assertTrue(McaVillagerCompat.isKnownMcaVillagerEntityPath("female_villager"));
        assertTrue(McaVillagerCompat.isKnownMcaVillagerEntityPath("male_zombie_villager"));
        assertTrue(McaVillagerCompat.isKnownMcaVillagerEntityPath("female_zombie_villager"));
    }

    @Test
    void nonMerchantMcaEntitiesAreExcluded() {
        assertTrue(McaVillagerCompat.isExcludedMcaEntityPath("grim_reaper"));
        assertTrue(McaVillagerCompat.isExcludedMcaEntityPath("crib"));
        assertFalse(McaVillagerCompat.isExcludedMcaEntityPath("male_villager"));
    }

    @Test
    void forwardCompatVillagerPathHeuristic() {
        assertTrue(McaVillagerCompat.isMcaVillagerEntityPath("custom_villager_npc"));
        assertFalse(McaVillagerCompat.isMcaVillagerEntityPath("grim_reaper"));
        assertFalse(McaVillagerCompat.isMcaVillagerEntityPath("something_else"));
    }
}
