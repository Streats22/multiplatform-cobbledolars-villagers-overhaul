package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaVillagerCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

/**
 * If vanilla opens {@link MerchantScreen} because of an MCA villager trader, reroute into the CobbleDollars shop.
 */
public final class FabricMerchantMcaRedirect {

    private FabricMerchantMcaRedirect() {
    }

    /**
     * @return {@code true} if the incoming screen must not apply (merchant opening was suppressed).
     */
    public static boolean suppressIncomingMerchantScreen(Screen screen) {
        if (!(screen instanceof MerchantScreen merchantScreen)) {
            return false;
        }
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI || !CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }
        if (!McaVillagerCompat.isModLoaded()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        MerchantMenu merchantMenu = merchantScreen.getMenu();
        Merchant trader = findMerchantForMenu(merchantMenu);
        if (!(trader instanceof Entity entity)) {
            return false;
        }
        if (!McaVillagerCompat.isMcaVillager(entity)) {
            return false;
        }
        if (!McaVillagerCompat.canTradeWithProfession(entity)) {
            return false;
        }
        if (entity instanceof Villager villagerEntity) {
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerEntity.getVillagerData().getProfession());
            if (Config.isVillagerProfessionExcluded(profId)) {
                return false;
            }
        }
        int traderId = entity.getId();
        FabricPendingCustomShopScreen.beginAwaitingShopData(traderId, true);
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(traderId));
        return true;
    }

    private static Merchant findMerchantForMenu(MerchantMenu menu) {
        if (menu == null) {
            return null;
        }
        for (Class<?> c = menu.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                if (Merchant.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        return (Merchant) f.get(menu);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
