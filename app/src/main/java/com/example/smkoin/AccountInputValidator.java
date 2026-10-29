package com.example.smkoin;

/** Shared, side-effect-free validation for account form input. */
final class AccountInputValidator {
    private AccountInputValidator() { }

    static String fullNameError(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) return "Enter your name.";
        if (name.length() > 100) return "Use 100 characters or fewer.";
        return null;
    }

    static String newPasswordError(String current, String next) {
        if (next == null || next.length() < 8) return "Use a password with at least 8 characters.";
        if (next.equals(current)) return "Choose a password different from your current password.";
        return null;
    }

    static String passwordConfirmationError(String next, String confirmation) {
        return next != null && next.equals(confirmation) ? null : "The passwords do not match.";
    }
}
