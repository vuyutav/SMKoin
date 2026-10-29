package com.example.smkoin;

import java.math.BigInteger;

/** Tracks a balance load without allowing an older account/request to overwrite newer state. */
final class PointBalancePresenter {
    enum Status { SIGN_IN_REQUIRED, LOADING, AVAILABLE, ERROR }

    static final class State {
        final Status status;
        final String userId;
        final Long balance;
        final String message;

        private State(Status status, String userId, Long balance, String message) {
            this.status = status;
            this.userId = userId;
            this.balance = balance;
            this.message = message;
        }
    }

    static final class Request {
        final long generation;
        final String userId;

        private Request(long generation, String userId) {
            this.generation = generation;
            this.userId = userId;
        }
    }

    private long generation;
    private String activeUserId;
    private State state = new State(Status.SIGN_IN_REQUIRED, null, null, "Sign in to view your points.");

    State state() { return state; }

    Request begin(String userId) {
        generation++;
        if (userId == null || userId.trim().isEmpty()) {
            activeUserId = null;
            state = new State(Status.SIGN_IN_REQUIRED, null, null, "Sign in to view your points.");
            return null;
        }
        activeUserId = userId;
        state = new State(Status.LOADING, userId, null, "Loading your point balance…");
        return new Request(generation, userId);
    }

    void setError(String message) {
        generation++;
        activeUserId = null;
        state = new State(Status.ERROR, null, null,
                message == null || message.trim().isEmpty() ? "Balance unavailable. Tap to retry." : message);
    }

    boolean complete(Request request, Long balance, Exception error) {
        if (request == null || request.generation != generation
                || !request.userId.equals(activeUserId)) return false;
        if (error != null || balance == null) {
            String message = error == null ? "The balance response was empty." : error.getMessage();
            state = new State(Status.ERROR, request.userId, null,
                    message == null || message.trim().isEmpty() ? "Balance unavailable. Tap to retry." : message);
            return true;
        }
        state = new State(Status.AVAILABLE, request.userId, balance, "Updated from your point ledger.");
        return true;
    }

    /** Integer percentage for a configured reward target; Home does not call this until a target exists. */
    static int progressPercent(long balance, long rewardTarget) {
        if (rewardTarget <= 0) throw new IllegalArgumentException("Reward target must be positive.");
        if (balance <= 0) return 0;
        if (balance >= rewardTarget) return 100;
        return BigInteger.valueOf(balance).multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(rewardTarget)).intValue();
    }
}
