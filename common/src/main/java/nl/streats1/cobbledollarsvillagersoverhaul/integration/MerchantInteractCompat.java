package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;

/**
 * Minecraft-facing wrapper around {@link MerchantInteractPolicy}.
 *
 * <p>Keeps {@code CobbleDollarsVillagersOverhaulRca} thin: extract registry ids here, apply
 * optional-mod / vanilla passthrough rules, never hard-crash if an optional mod is absent.
 */
public final class MerchantInteractCompat {

    private MerchantInteractCompat() {
    }

    /**
     * @return {@code true} if this interact must be left to vanilla or another mod (do not open shop).
     */
    public static boolean shouldDeferShopOverride(Entity target, boolean sneaking, ItemStack heldItem) {
        if (target == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (typeId != null && MerchantInteractPolicy.isEntityTypeExcluded(
                typeId.getNamespace(),
                typeId.getPath(),
                Config.EXCLUDED_ENTITY_TYPE_IDS,
                Config.EXCLUDED_ENTITY_TYPE_NAMESPACES)) {
            return true;
        }
        if (MerchantInteractPolicy.shouldSkipWhenSneaking(sneaking, Config.SKIP_SHOP_OVERRIDE_WHEN_SNEAKING)) {
            return true;
        }
        if (heldItem == null || heldItem.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        if (itemId == null) {
            return false;
        }
        boolean namedNameTag = heldItem.get(DataComponents.CUSTOM_NAME) != null;
        return MerchantInteractPolicy.shouldPassthroughHeldItem(
                itemId.getNamespace(),
                itemId.getPath(),
                namedNameTag,
                Config.PASSTHROUGH_INTERACT_ITEM_IDS,
                Config.PASSTHROUGH_INTERACT_ITEM_NAMESPACES);
    }
}
