package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.ModMenuTypes;
import nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.VillagerShopMenu;

/**
 * Registers the shop menu type so the client can create it when receiving ShopData (allows moving items).
 */
public final class NeoForgeMenuRegistration {

    public static void register() {
        @SuppressWarnings("unchecked")
        MenuType<VillagerShopMenu>[] holder = new MenuType[1];
        holder[0] = new MenuType<>((syncId, inv) ->
                new VillagerShopMenu(holder[0], syncId, inv, -1, 0L, java.util.List.of(), java.util.List.of(), java.util.List.of(), false), FeatureFlags.DEFAULT_FLAGS);
        net.minecraft.core.Registry.register(BuiltInRegistries.MENU, ModMenuTypes.VILLAGER_SHOP_MENU_ID, holder[0]);
        ModMenuTypes.setVillagerShopMenu(holder[0]);
    }
}
