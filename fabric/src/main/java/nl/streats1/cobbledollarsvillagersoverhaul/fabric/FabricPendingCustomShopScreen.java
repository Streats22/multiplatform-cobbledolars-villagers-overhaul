package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.mixin.MerchantMenuTraderAccessor;

/**
 * After the client sends {@link nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads.RequestShopData},
 * vanilla may still open {@link MerchantScreen} briefly on dedicated servers before {@code ShopData} arrives.
 * Suppress that merchant GUI for the same entity until the custom shop opens or the window expires.
 * Integrated singleplayer is skipped: packets are synchronous there and suppression tends to cause flicker instead.
 */
public final class FabricPendingCustomShopScreen {
    /**
     * ~2s at 20 TPS — enough for laggy servers; then allow vanilla if nothing arrived.
     */
    private static final int TICKS_TO_LIVE = 45;

    private static int pendingEntityId = Integer.MIN_VALUE;
    private static int ticksLeft;

    private FabricPendingCustomShopScreen() {
    }

    public static void beginAwaitingShopData(int villagerEntityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return;
        }
        pendingEntityId = villagerEntityId;
        ticksLeft = TICKS_TO_LIVE;
    }

    public static void clear(String reason) {
        if (pendingEntityId != Integer.MIN_VALUE) {
            pendingEntityId = Integer.MIN_VALUE;
            ticksLeft = 0;
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug("[shop] Fabric pending shop: cleared ({})", reason);
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

    /**
     * Called when {@code ShopData} is applied so the next frame is not blocked.
     */
    public static void onShopDataReceived(int villagerId) {
        if (pendingEntityId == villagerId) {
            clear("shop-data-received");
        }
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
            if (!(menu instanceof MerchantMenuTraderAccessor accessor)) {
                return false;
            }
            Merchant trader = accessor.cobbledollars_villagers_overhaul_rca$getTrader();
            if (trader instanceof Entity entity && entity.getId() == pendingEntityId) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
