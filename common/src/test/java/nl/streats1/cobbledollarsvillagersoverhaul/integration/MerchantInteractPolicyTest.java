package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantInteractPolicyTest {

    @Test
    void sneakSkipsShopWhenEnabled() {
        assertTrue(MerchantInteractPolicy.shouldSkipWhenSneaking(true, true));
        assertFalse(MerchantInteractPolicy.shouldSkipWhenSneaking(true, false));
        assertFalse(MerchantInteractPolicy.shouldSkipWhenSneaking(false, true));
    }

    @Test
    void spawnEggsAlwaysPassthrough() {
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "minecraft", "villager_spawn_egg", false, List.of(), List.of()));
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "mca", "male_villager_spawn_egg", false, List.of(), List.of()));
    }

    @Test
    void namedNameTagPassthroughUnnamedDoesNot() {
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "minecraft", "name_tag", true, List.of(), List.of()));
        assertFalse(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "minecraft", "name_tag", false, List.of(), List.of()));
    }

    @Test
    void leadWithoutConfigDoesNotPassthrough() {
        assertFalse(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "minecraft", "lead", false, List.of(), List.of()));
    }

    @Test
    void defaultCompanionNamespacesCoverLassosAndBackpacks() {
        var namespaces = ModConfigDefaults.DEFAULT_PASSTHROUGH_INTERACT_ITEM_NAMESPACES;
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "moblassos", "golden_lasso", false, List.of(), namespaces));
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "sophisticatedbackpacks", "backpack", false, List.of(), namespaces));
        assertFalse(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "minecraft", "emerald", false, List.of(), namespaces));
    }

    @Test
    void configuredItemIdPassthroughIsCaseInsensitive() {
        assertTrue(MerchantInteractPolicy.shouldPassthroughHeldItem(
                "Cyclic", "spawner_spawner", false, List.of("cyclic:SPAWNER_SPAWNER"), List.of()));
    }

    @Test
    void emptyHandHasNoItemPassthrough() {
        assertFalse(MerchantInteractPolicy.shouldPassthroughHeldItem(
                null, null, false, List.of("minecraft:lead"), List.of("moblassos")));
    }

    @Test
    void excludedEntityTypeIdAndNamespace() {
        assertTrue(MerchantInteractPolicy.isEntityTypeExcluded(
                "goblintraders", "goblin_trader", List.of("goblintraders:goblin_trader"), List.of()));
        assertTrue(MerchantInteractPolicy.isEntityTypeExcluded(
                "customnpcs", "trader", List.of(), List.of("customnpcs")));
        assertFalse(MerchantInteractPolicy.isEntityTypeExcluded(
                "minecraft", "villager", List.of("goblintraders:goblin_trader"), List.of("customnpcs")));
    }
}
