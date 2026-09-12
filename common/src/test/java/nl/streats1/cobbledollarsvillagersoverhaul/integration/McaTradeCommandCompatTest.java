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
        assertTrue(handler.interactingPlayerStillSet());
    }

    @Test
    void clearInteractingPlayerOnlyDoesNotCallStopInteracting() {
        FakeHandler handler = new FakeHandler(new Object());
        handler.interactingPlayer = new Object();
        McaTradeCommandCompat.clearInteractingPlayerOnly(handler);
        assertFalse(handler.stopInteractingCalled);
        assertNull(handler.interactingPlayer);
    }

    /** Minimal stand-in for MCA EntityCommandHandler shape. */
    @SuppressWarnings("unused")
    private static final class FakeHandler {
        protected final Object entity;
        protected Object interactingPlayer = new Object();
        boolean stopInteractingCalled;

        FakeHandler(Object entity) {
            this.entity = entity;
        }

        public void stopInteracting() {
            stopInteractingCalled = true;
            interactingPlayer = null;
        }

        boolean interactingPlayerStillSet() {
            return interactingPlayer != null;
        }
    }
}
