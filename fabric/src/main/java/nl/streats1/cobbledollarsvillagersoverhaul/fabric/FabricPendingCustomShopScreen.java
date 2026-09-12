package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

public final class FabricPendingCustomShopScreen {
    private static final int TICKS_TO_LIVE = 45;

    private static int pendingEntityId = Integer.MIN_VALUE;
    private static int ticksLeft;

    private FabricPendingCustomShopScreen() {
    }

    public static void beginAwaitingShopData(int villagerEntityId) {
        beginAwaitingShopData(villagerEntityId, false);
    }

    public static void beginAwaitingShopData(int villagerEntityId, boolean bypassSingleplayerGate) {
        Minecraft mc = Minecraft.getInstance();
        if (!bypassSingleplayerGate && mc.getSingleplayerServer() != null) {
            return;
        }
        pendingEntityId = villagerEntityId;
        ticksLeft = TICKS_TO_LIVE;
    }

    public static void clear(String reason) {
        if (pendingEntityId != Integer.MIN_VALUE) {
            pendingEntityId = Integer.MIN_VALUE;
            ticksLeft = 0;
        }
    }

    public static void onClientTick() {
        if (ticksLeft > 0) {
            ticksLeft--;
            if (ticksLeft <= 0) {
                clear("ttl-expired");
            }
        }
    }

    public static void onShopDataReceived(int villagerId) {
        if (pendingEntityId == villagerId) {
            clear("shop-data-received");
        }
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

    public static boolean shouldSuppressMerchantScreen(Screen screen) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return false;
        }
        if (!(screen instanceof MerchantScreen merchantScreen)) {
            return false;
        }
        if (ticksLeft <= 0 || pendingEntityId == Integer.MIN_VALUE) {
            return false;
        }
        try {
            var menu = merchantScreen.getMenu();
            if (!(menu instanceof MerchantMenu merchantMenu)) {
                return false;
            }
            Merchant trader = findMerchantForMenu(merchantMenu);
            if (trader instanceof Entity entity && entity.getId() == pendingEntityId) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
