package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaTradeCommandCompatTest {

    @Test
    void onlyTradeCommandIsRecognized() {
        assertTrue(McaTradeCommandCompat.isTradeCommand("trade"));
        assertFalse(McaTradeCommandCompat.isTradeCommand("Trade"));
        assertFalse(McaTradeCommandCompat.isTradeCommand("inventory"));
        assertFalse(McaTradeCommandCompat.isTradeCommand(null));
        assertFalse(McaTradeCommandCompat.isTradeCommand(""));
    }

    @Test
    void findEntityFieldValueReadsProtectedEntityField() {
        Object marker = new Object();
        FakeHandler handler = new FakeHandler(marker);
        assertSame(marker, McaTradeCommandCompat.findEntityFieldValue(handler));
    }

    @Test
    void findEntityFieldValueNullSafe() {
        assertNull(McaTradeCommandCompat.findEntityFieldValue(null));
        assertNull(McaTradeCommandCompat.findEntityFieldValue(new Object()));
    }

    @Test
    void stopInteractingInvokesMethodWhenPresent() {
        FakeHandler handler = new FakeHandler(new Object());
        McaTradeCommandCompat.stopInteracting(handler);
        assertTrue(handler.stopInteractingCalled);
    }

    @Test
    void tryRedirectTradeIgnoresNonTradeCommands() {
        FakeHandler handler = new FakeHandler(new Object());
        assertFalse(McaTradeCommandCompat.tryRedirectTrade(handler, null, "inventory"));
        assertFalse(handler.stopInteractingCalled);
    }

    /** Minimal stand-in for MCA EntityCommandHandler shape. */
    @SuppressWarnings("unused")
    private static final class FakeHandler {
        protected final Object entity;
        boolean stopInteractingCalled;

        FakeHandler(Object entity) {
            this.entity = entity;
        }

        public void stopInteracting() {
            stopInteractingCalled = true;
        }
    }
}
