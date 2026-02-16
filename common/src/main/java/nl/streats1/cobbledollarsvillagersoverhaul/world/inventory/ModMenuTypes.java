package nl.streats1.cobbledollarsvillagersoverhaul.world.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

/**
 * Menu types for the CobbleDollars shop.
 * Platform (Fabric/NeoForge) sets the MenuType via setVillagerShopMenu.
 */
public final class ModMenuTypes {

    public static final ResourceLocation VILLAGER_SHOP_MENU_ID = ResourceLocation.fromNamespaceAndPath(CobbleDollarsVillagersOverhaulRca.MOD_ID, "villager_shop");

    private static MenuType<VillagerShopMenu> villagerShopMenuType;

    private ModMenuTypes() {
    }

    /**
     * Called by platform init.
     */
    public static void setVillagerShopMenu(MenuType<VillagerShopMenu> type) {
        villagerShopMenuType = type;
    }

    public static MenuType<VillagerShopMenu> getVillagerShopMenu() {
        return villagerShopMenuType;
    }
}
