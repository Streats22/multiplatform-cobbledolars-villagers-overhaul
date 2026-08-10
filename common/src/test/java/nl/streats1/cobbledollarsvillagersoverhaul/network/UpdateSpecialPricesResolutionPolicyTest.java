package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Fabric intermediary exposes {@code startTrading} (method_19191) before {@code updateSpecialPrices}
 * (method_19192) as {@code void(Player)}. Blind "first match" binding opens a vanilla merchant menu
 * on every custom-shop open/buy/sell — resolve by known safe names only.
 */
class UpdateSpecialPricesResolutionPolicyTest {

    @Test
    void prefersMojmapName() {
        assertEquals("updateSpecialPrices",
                CobbleDollarsShopPayloadHandlers.selectUpdateSpecialPricesMethodName(
                        List.of("startTrading", "updateSpecialPrices", "setTradingPlayer")));
    }

    @Test
    void fabricIntermediaryPicksMethod19192NotStartTrading() {
        // Class-file order on 1.21.1 client: method_19191 (startTrading) then method_19192
        assertEquals("method_19192",
                CobbleDollarsShopPayloadHandlers.selectUpdateSpecialPricesMethodName(
                        List.of("method_19191", "method_19192")));
    }

    @Test
    void yarnPrepareOffersForAccepted() {
        assertEquals("prepareOffersFor",
                CobbleDollarsShopPayloadHandlers.selectUpdateSpecialPricesMethodName(
                        List.of("beginTradeWith", "prepareOffersFor")));
    }

    @Test
    void doesNotFallBackToStartTradingWhenReputationMethodMissing() {
        assertNull(CobbleDollarsShopPayloadHandlers.selectUpdateSpecialPricesMethodName(
                List.of("startTrading", "method_19191", "setTradingPlayer")));
    }
}
