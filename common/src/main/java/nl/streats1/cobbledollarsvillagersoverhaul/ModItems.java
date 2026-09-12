package nl.streats1.cobbledollarsvillagersoverhaul;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModItems {

    private ModItems() {
    }

    private static Item cachedSign = null;

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
