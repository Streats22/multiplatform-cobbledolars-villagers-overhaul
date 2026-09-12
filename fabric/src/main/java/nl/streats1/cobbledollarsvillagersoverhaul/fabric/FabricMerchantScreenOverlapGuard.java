package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

public final class FabricMerchantScreenOverlapGuard {

    private static final int WINDOW_TICKS = 45;
    private static final int MAX_REOPEN = 16;
        private static final int RECOVERY_START_TICK = 2;

    private static int ticksRemaining;
    private static int ticksSinceArm;
    private static int expectedEntityId = Integer.MIN_VALUE;
    private static int reopenAttempts;
    private static int earlyNullRecoveryAttempts;
    private static CobbleDollarsShopPayloads.ShopData cachedPayload;

    private FabricMerchantScreenOverlapGuard() {
    }

    public static void arm(CobbleDollarsShopPayloads.ShopData payload) {
        cachedPayload = payload;
        expectedEntityId = payload.villagerId();
        ticksRemaining = WINDOW_TICKS;
        ticksSinceArm = 0;
        reopenAttempts = 0;
        earlyNullRecoveryAttempts = 0;
    }

        public static void clear() {
        clear("disconnect");
    }

    private static void clear(String reason) {
        boolean hadPayload = cachedPayload != null;
        cachedPayload = null;
        ticksRemaining = 0;
        ticksSinceArm = 0;
        expectedEntityId = Integer.MIN_VALUE;
        reopenAttempts = 0;
        earlyNullRecoveryAttempts = 0;
        if (hadPayload) {
        }
    }

        public static void scheduleDeferredRecheck(Minecraft mc) {
        mc.execute(() -> {
            if (cachedPayload == null) {
                return;
            }
            if (mc.level == null) {
                return;
            }
            if (alreadyShowingOurShop(mc)) {
                return;
            }
            String screenName = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
            if (mc.screen instanceof MerchantScreen || mc.screen == null) {
                reopenFromCache(mc, "deferred-recheck");
            } else {
            }
        });
    }

    static void onEndClientTick(Minecraft mc) {
        if (cachedPayload == null) {
            return;
        }
        ticksSinceArm++;
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (alreadyShowingOurShop(mc)) {
            return;
        }

        if (mc.screen instanceof CobbleDollarsShopScreen shop) {
        }

        if (mc.screen instanceof PauseScreen || mc.screen instanceof TitleScreen) {
            return;
        }

        if (ticksRemaining <= 0) {
            clear("window-expired");
            return;
        }

        if (reopenAttempts >= MAX_REOPEN) {
            clear("max-reopens");
            return;
        }

        
        if (mc.screen == null && mc.player != null && mc.level != null
                && ticksSinceArm >= RECOVERY_START_TICK && ticksSinceArm <= 5 && earlyNullRecoveryAttempts < 1) {
            earlyNullRecoveryAttempts++;
            reopenFromCache(mc, "early-null");
            ticksRemaining = Math.max(ticksRemaining, 28);
            return;
        }

        if (ticksSinceArm >= RECOVERY_START_TICK
                && mc.screen instanceof MerchantScreen merchantScreen
                && shouldReopenOverMerchant(merchantScreen)) {
            var menu = merchantScreen.getMenu();
            reopenFromCache(mc, "merchant-screen");
            ticksRemaining = Math.max(ticksRemaining, 22);
        }
    }

    private static boolean shouldReopenOverMerchant(MerchantScreen screen) {
        try {
            if (!(screen.getMenu() instanceof MerchantMenu)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        
        
        return true;
    }

    private static boolean alreadyShowingOurShop(Minecraft mc) {
        return mc.screen instanceof CobbleDollarsShopScreen s && s.shopTargetEntityId() == expectedEntityId;
    }

    private static void reopenFromCache(Minecraft mc, String reason) {
        CobbleDollarsShopPayloads.ShopData p = cachedPayload;
        if (p == null || mc.level == null) {
            return;
        }
        if (mc.screen instanceof CobbleDollarsShopScreen s && s.shopTargetEntityId() == p.villagerId()) {
            return;
        }
        String screenBefore = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
        reopenAttempts++;
        CobbleDollarsShopScreen.openFromPayload(
                p.villagerId(),
                p.balance(),
                p.buyOffers(),
                p.sellOffers(),
                p.tradesOffers(),
                p.buyOffersFromConfig(),
                p.canCycleTrades());
        String screenAfter = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
    }
}
