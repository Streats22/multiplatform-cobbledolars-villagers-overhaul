package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigShopBuySessionTest {

    @AfterEach
    void clearSessions() {
        ConfigShopBuySession.clear();
    }

    @Test
    void configCatalogStaysRecordedAfterOffersAppearAndIsNotImpliedByAnotherPlayer() {
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        UUID merchant = UUID.randomUUID();

        ConfigShopBuySession.remember(player, merchant, true);

        assertTrue(ConfigShopBuySession.wasOpenedAsConfigShop(player, merchant));
        assertFalse(ConfigShopBuySession.wasOpenedAsConfigShop(otherPlayer, merchant));
    }

    @Test
    void professionShopOpenClearsAPriorConfigCatalogForThatMerchant() {
        UUID player = UUID.randomUUID();
        UUID merchant = UUID.randomUUID();
        UUID otherMerchant = UUID.randomUUID();

        ConfigShopBuySession.remember(player, merchant, true);
        ConfigShopBuySession.remember(player, otherMerchant, true);
        ConfigShopBuySession.remember(player, merchant, false);

        assertFalse(ConfigShopBuySession.wasOpenedAsConfigShop(player, merchant));
        assertTrue(ConfigShopBuySession.wasOpenedAsConfigShop(player, otherMerchant));
    }

    @Test
    void forgetAndDisconnectDropTheRecordedCatalog() {
        UUID player = UUID.randomUUID();
        UUID merchant = UUID.randomUUID();
        ConfigShopBuySession.remember(player, merchant, true);

        ConfigShopBuySession.forget(player, merchant);
        assertFalse(ConfigShopBuySession.wasOpenedAsConfigShop(player, merchant));

        ConfigShopBuySession.remember(player, merchant, true);
        ConfigShopBuySession.forgetPlayer(player);
        assertFalse(ConfigShopBuySession.wasOpenedAsConfigShop(player, merchant));
    }

    @Test
    void spoofedClientFlagWithoutAnOpenCatalogIsNotRecorded() {
        UUID player = UUID.randomUUID();
        UUID merchant = UUID.randomUUID();

        assertFalse(ConfigShopBuySession.wasOpenedAsConfigShop(player, merchant));
        assertFalse(ShopTradePolicy.shouldBuyFromConfigShop(false, false, false));
    }
}
