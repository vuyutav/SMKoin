package com.example.smkoin;

import org.junit.Test;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PointBalancePresenterTest {
    @Test public void zeroAndPositiveBalancesAreAvailableValues() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request zero = presenter.begin("user-a");
        assertTrue(presenter.complete(zero, 0L, null));
        assertEquals(PointBalancePresenter.Status.AVAILABLE, presenter.state().status);
        assertEquals(Long.valueOf(0), presenter.state().balance);

        PointBalancePresenter.Request positive = presenter.begin("user-a");
        assertTrue(presenter.complete(positive, 275L, null));
        assertEquals(Long.valueOf(275), presenter.state().balance);
    }

    @Test public void refreshedBalanceReplacesOldValueAfterLedgerChange() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request beforeTransaction = presenter.begin("user-a");
        presenter.complete(beforeTransaction, 20L, null);

        PointBalancePresenter.Request afterTransaction = presenter.begin("user-a");
        assertEquals(PointBalancePresenter.Status.LOADING, presenter.state().status);
        assertNull(presenter.state().balance);
        assertTrue(presenter.complete(afterTransaction, 45L, null));
        assertEquals(Long.valueOf(45), presenter.state().balance);
    }

    @Test public void rewardProgressIsClampedAtBothBoundaries() {
        assertEquals(0, PointBalancePresenter.progressPercent(0, 100));
        assertEquals(0, PointBalancePresenter.progressPercent(-5, 100));
        assertEquals(50, PointBalancePresenter.progressPercent(50, 100));
        assertEquals(100, PointBalancePresenter.progressPercent(100, 100));
        assertEquals(100, PointBalancePresenter.progressPercent(Long.MAX_VALUE, 100));
        assertThrows(IllegalArgumentException.class, () -> PointBalancePresenter.progressPercent(1, 0));
    }

    @Test public void missingOrFailedBalanceResponseIsNotPresentedAsZero() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request request = presenter.begin("user-a");
        assertTrue(presenter.complete(request, null, new Exception("Network unavailable")));
        assertEquals(PointBalancePresenter.Status.ERROR, presenter.state().status);
        assertNull(presenter.state().balance);

        assertThrows(IllegalStateException.class,
                () -> SupabaseRepository.parseBalance("user-a", 0, null, null));
        assertThrows(IllegalStateException.class,
                () -> SupabaseRepository.parseBalance("user-a", 1, "user-a", null));
    }

    @Test public void balanceParserRequiresExactOwnerAndWholeNumber() {
        assertEquals(123L, SupabaseRepository.parseBalance("user-a", 1, "user-a", "123"));
        assertThrows(IllegalStateException.class,
                () -> SupabaseRepository.parseBalance("user-b", 1, "user-a", "123"));
        assertThrows(IllegalStateException.class,
                () -> SupabaseRepository.parseBalance("user-a", 1, "user-a", "1.5"));
    }

    @Test public void returningToHomeStartsFreshLoadAndIgnoresOlderResponse() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request firstVisit = presenter.begin("user-a");
        assertTrue(presenter.complete(firstVisit, 10L, null));

        PointBalancePresenter.Request returnVisit = presenter.begin("user-a");
        assertEquals(PointBalancePresenter.Status.LOADING, presenter.state().status);
        assertNull(presenter.state().balance);
        assertFalse(presenter.complete(firstVisit, 10L, null));
        assertTrue(presenter.complete(returnVisit, 18L, null));
        assertEquals(Long.valueOf(18), presenter.state().balance);
    }

    @Test public void accountSwitchClearsOldBalanceAndRejectsOldAccountResponse() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request userA = presenter.begin("user-a");
        presenter.complete(userA, 600L, null);

        PointBalancePresenter.Request userB = presenter.begin("user-b");
        assertEquals(PointBalancePresenter.Status.LOADING, presenter.state().status);
        assertEquals("user-b", presenter.state().userId);
        assertNull(presenter.state().balance);
        assertFalse(presenter.complete(userA, 600L, null));
        assertTrue(presenter.complete(userB, 0L, null));
        assertEquals(Long.valueOf(0), presenter.state().balance);
        assertEquals("user-b", presenter.state().userId);
    }

    @Test public void balanceClearsWhenThereIsNoCurrentSession() {
        PointBalancePresenter presenter = new PointBalancePresenter();
        PointBalancePresenter.Request signedIn = presenter.begin("user-a");
        presenter.complete(signedIn, 700L, null);

        assertNull(presenter.begin(null));
        assertEquals(PointBalancePresenter.Status.SIGN_IN_REQUIRED, presenter.state().status);
        assertNull(presenter.state().balance);
        assertFalse(presenter.complete(signedIn, 700L, null));
    }

    @Test public void redemptionFailureClassificationAvoidsBlindRetryAfterUncertainOutcome() {
        assertFalse(SupabaseRepository.redemptionFailureMayHaveCommitted(
                new SupabaseClient.RequestException(400, "Insufficient stock")));
        assertTrue(SupabaseRepository.redemptionFailureMayHaveCommitted(
                new SupabaseClient.RequestException(503, "Service unavailable")));
        assertTrue(SupabaseRepository.redemptionFailureMayHaveCommitted(new IOException("Timed out")));
        assertFalse(SupabaseRepository.redemptionFailureMayHaveCommitted(
                new Exception("Your session expired. Please sign in again.")));
    }
}
