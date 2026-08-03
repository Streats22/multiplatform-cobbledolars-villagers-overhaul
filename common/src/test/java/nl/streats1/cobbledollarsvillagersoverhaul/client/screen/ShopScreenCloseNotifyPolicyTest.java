package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards merchant release on shop teardown: {@code ShopScreenClosed} must be eligible from
 * {@code Screen.removed()}, not only {@code onClose()}.
 * <p>
 * Concrete failure mode before the fix: after any buy/sell the server keeps
 * {@code AbstractVillager.tradingPlayer} set; opening the bank, dying, swapping to another shop,
 * or opening the editor replaces the screen via {@code Minecraft.setScreen} which calls
 * {@code removed()} and never {@code onClose()}. Merchants stayed {@code isTrading()} until logout
 * (wandering traders skip despawn; other players locked out).
 */
class ShopScreenCloseNotifyPolicyTest {

    @Test
    void realMerchantNotifiesWhenConnected() {
        assertTrue(CobbleDollarsShopScreen.shouldNotifyShopClosedOnRemoved(42, true));
    }

    @Test
    void virtualShopsNeverNotify() {
        assertFalse(CobbleDollarsShopScreen.shouldNotifyShopClosedOnRemoved(VirtualShopIds.VIRTUAL_ID_SHOP, true));
        assertFalse(CobbleDollarsShopScreen.shouldNotifyShopClosedOnRemoved(VirtualShopIds.VIRTUAL_ID_BANK, true));
    }

    @Test
    void offlineClientDoesNotNotify() {
        assertFalse(CobbleDollarsShopScreen.shouldNotifyShopClosedOnRemoved(42, false));
    }
}
