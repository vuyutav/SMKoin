package com.example.smkoin;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

/** Validates the current Supabase session before any protected activity renders. */
final class AuthGate {
    private AuthGate() { }

    static void requireSession(AppCompatActivity activity, Runnable onAuthenticated) {
        SupabaseRepository.get(activity).validateSession((valid, error) -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (error == null && Boolean.TRUE.equals(valid)) {
                onAuthenticated.run();
                return;
            }

            redirect(activity, error);
        });
    }

    static void redirect(AppCompatActivity activity, Exception error) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Intent destination;
        if (error == null) {
            destination = new Intent(activity, AuthActivity.class);
            destination.putExtra(AuthActivity.EXTRA_AUTH_ENTRY, true);
        } else {
            // Preserve a possibly recoverable refresh token and let Splash retry verification.
            destination = new Intent(activity, SplashActivity.class);
        }
        destination.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(destination);
        activity.finish();
    }
}
