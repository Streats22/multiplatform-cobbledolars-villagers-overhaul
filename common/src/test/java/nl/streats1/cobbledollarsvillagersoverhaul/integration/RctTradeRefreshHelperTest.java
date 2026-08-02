package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RctTradeRefreshHelperTest {

    @SuppressWarnings("unused")
    private static final class FakeTrainer {
        void updateTrades() {
        }

        void method_12345() {
        }

        void method_withArg(int ignored) {
        }

        int method_returnsInt() {
            return 1;
        }

        static void method_static() {
        }
    }

    @Test
    void prefersNamedUpdateTradesOverIntermediaryMethods() throws Exception {
        Method[] methods = FakeTrainer.class.getDeclaredMethods();
        List<Method> selected = RctTradeRefreshHelper.selectUpdateTradesCandidates(methods);
        assertEquals(1, selected.size());
        assertEquals("updateTrades", selected.get(0).getName());
    }

    @Test
    void rejectsParameterizedAndNonVoidCandidates() throws Exception {
        Method withArg = FakeTrainer.class.getDeclaredMethod("method_withArg", int.class);
        Method returnsInt = FakeTrainer.class.getDeclaredMethod("method_returnsInt");
        Method staticMethod = FakeTrainer.class.getDeclaredMethod("method_static");
        assertFalse(RctTradeRefreshHelper.isSafeUpdateTradesCandidate(withArg));
        assertFalse(RctTradeRefreshHelper.isSafeUpdateTradesCandidate(returnsInt));
        assertFalse(RctTradeRefreshHelper.isSafeUpdateTradesCandidate(staticMethod));
        assertTrue(RctTradeRefreshHelper.isSafeUpdateTradesCandidate(
                FakeTrainer.class.getDeclaredMethod("updateTrades")));
        assertTrue(RctTradeRefreshHelper.isSafeUpdateTradesCandidate(
                FakeTrainer.class.getDeclaredMethod("method_12345")));
    }
}
