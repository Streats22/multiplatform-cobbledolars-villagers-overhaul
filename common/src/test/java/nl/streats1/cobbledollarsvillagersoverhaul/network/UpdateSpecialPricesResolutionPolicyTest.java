package nl.streats1.cobbledollarsvillagersoverhaul.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateSpecialPricesResolutionPolicyTest {

    @Test
    void prefersMojmapName() {
        assertEquals("updateSpecialPrices",
                ShopTradeSession.selectUpdateSpecialPricesMethodName(
                        List.of("startTrading", "updateSpecialPrices", "setTradingPlayer")));
    }

    @Test
    void fabricIntermediaryPicksMethod19192NotStartTrading() {
        assertEquals("method_19192",
                ShopTradeSession.selectUpdateSpecialPricesMethodName(
                        List.of("method_19191", "method_19192")));
    }

    @Test
    void yarnPrepareOffersForAccepted() {
        assertEquals("prepareOffersFor",
                ShopTradeSession.selectUpdateSpecialPricesMethodName(
                        List.of("beginTradeWith", "prepareOffersFor")));
    }

    @Test
    void doesNotFallBackToStartTradingWhenReputationMethodMissing() {
        assertNull(ShopTradeSession.selectUpdateSpecialPricesMethodName(
                List.of("startTrading", "method_19191", "setTradingPlayer")));
    }
}
