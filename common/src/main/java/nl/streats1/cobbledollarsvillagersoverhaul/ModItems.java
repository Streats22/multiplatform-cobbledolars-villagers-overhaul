package nl.streats1.cobbledollarsvillagersoverhaul;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Virtual item used solely to carry a custom BakedModel (3-D CobbleDollar coin).
 * The item is never obtainable; it exists so the resource-pack model pipeline can
 * resolve {@code cobbledollars_villagers_overhaul_rca:cobbledollar_sign}.
 */
public final class ModItems {

    private ModItems() {
    }

    private static Item cachedSign = null;

    /**
     * Returns the registered {@code cobbledollar_sign} item, or {@code null}
     * if it has not been registered yet (should not happen during gameplay).
     * Result is cached after the first successful lookup.
     */
    public static Item getCobbleDollarSign() {
        if (cachedSign == null) {
            cachedSign = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath(
                            CobbleDollarsVillagersOverhaulRca.MOD_ID, "cobbledollar_sign")
            ).orElse(null);
        }
        return cachedSign;
    }
}
