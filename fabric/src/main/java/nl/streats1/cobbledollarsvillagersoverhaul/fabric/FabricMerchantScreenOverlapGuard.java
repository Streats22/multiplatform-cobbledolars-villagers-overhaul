package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

/**
 * Vanilla merchant packets or other mods can replace or clear the CobbleDollars shop right after
 * {@link CobbleDollarsShopPayloads.ShopData}. Re-open from the cached payload when we detect that
 * within a short window.
 */
public final class FabricMerchantScreenOverlapGuard {

    private static final int WINDOW_TICKS = 45;
    private static final int MAX_REOPEN = 16;

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
        CobbleDollarsVillagersOverhaulRca.LOGGER.info(
                "[shop] Fabric guard: armed villagerId={} (overlap recovery, {} tick window)",
                expectedEntityId, WINDOW_TICKS);
    }

    /** Clears guard state (e.g. disconnect). Logged at debug if a payload was armed. */
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
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug("[shop] Fabric guard: cleared ({})", reason);
        }
    }

    /**
     * Run after the main {@link CobbleDollarsShopPayloads.ShopData} handler, on the next client work queue step,
     * so packets processed later in the same tick cannot wipe the screen before we correct it.
     */
    public static void scheduleDeferredRecheck(Minecraft mc) {
        mc.execute(() -> {
            if (cachedPayload == null) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric guard: deferred-recheck skipped (no cached payload)");
                return;
            }
            if (mc.level == null) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric guard: deferred-recheck skipped (level null)");
                return;
            }
            if (mc.screen instanceof CobbleDollarsShopScreen s && s.shopTargetEntityId() == expectedEntityId) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric guard: deferred-recheck ok (shop already open villagerId={})",
                        expectedEntityId);
                return;
            }
            String screenName = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
            if (mc.screen instanceof MerchantScreen || mc.screen == null) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric guard: deferred-recheck reopening (screen={})",
                        screenName);
                reopenFromCache(mc, "deferred-recheck");
            } else {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                        "[shop] Fabric guard: deferred-recheck skipped unexpected screen={}",
                        screenName);
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

        if (mc.screen instanceof CobbleDollarsShopScreen shop) {
            if (shop.shopTargetEntityId() == expectedEntityId) {
                return;
            }
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] Fabric guard: tick armed but open shop is other villager (open={} expected={})",
                    shop.shopTargetEntityId(), expectedEntityId);
        }

        if (mc.screen instanceof PauseScreen || mc.screen instanceof TitleScreen) {
            return;
        }

        if (ticksRemaining <= 0) {
            clear("window-expired");
            return;
        }

        if (reopenAttempts >= MAX_REOPEN) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn(
                    "[shop] Fabric guard: gave up after {} reopen attempts (entity id {})",
                    MAX_REOPEN, expectedEntityId);
            clear("max-reopens");
            return;
        }

        // Instant close: no GUI at all (first ~2 ticks only — normal gameplay is screen==null too).
        if (mc.screen == null && mc.player != null && mc.level != null
                && ticksSinceArm <= 2 && earlyNullRecoveryAttempts < 4) {
            earlyNullRecoveryAttempts++;
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] Fabric guard: early-null recovery ticksSinceArm={} attempt={}/4 ticksRemaining={}",
                    ticksSinceArm, earlyNullRecoveryAttempts, ticksRemaining);
            reopenFromCache(mc, "early-null");
            ticksRemaining = Math.max(ticksRemaining, 28);
            return;
        }

        if (mc.screen instanceof MerchantScreen merchantScreen && shouldReopenOverMerchant(merchantScreen)) {
            var menu = merchantScreen.getMenu();
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] Fabric guard: merchant screen during armed window (menu={}), reopening ticksSinceArm={}",
                    menu != null ? menu.getClass().getSimpleName() : "null", ticksSinceArm);
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
        // Any vanilla-style merchant menu during an armed shop window: prefer CobbleDollars UI.
        // Strict entity-id matching was too brittle on some clients (reflection / modded menus).
        return true;
    }

    private static void reopenFromCache(Minecraft mc, String reason) {
        CobbleDollarsShopPayloads.ShopData p = cachedPayload;
        if (p == null || mc.level == null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                    "[shop] Fabric guard: reopenFromCache aborted ({}) payloadNull={} levelNull={}",
                    reason, p == null, mc.level == null);
            return;
        }
        String screenBefore = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
        reopenAttempts++;
        CobbleDollarsVillagersOverhaulRca.LOGGER.info(
                "[shop] Fabric guard: reopen CobbleDollars shop ({}) attempt {}/{} villagerId={} screenBefore={}",
                reason, reopenAttempts, MAX_REOPEN, p.villagerId(), screenBefore);
        CobbleDollarsShopScreen.openFromPayload(
                p.villagerId(),
                p.balance(),
                p.buyOffers(),
                p.sellOffers(),
                p.tradesOffers(),
                p.buyOffersFromConfig(),
                p.canCycleTrades());
        String screenAfter = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
        CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                "[shop] Fabric guard: after reopen screen={} (expected CobbleDollarsShopScreen)",
                screenAfter);
    }
}
