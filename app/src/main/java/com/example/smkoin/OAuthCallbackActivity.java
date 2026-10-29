package com.example.smkoin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/** Validates the Supabase OAuth callback and exchanges its one-time PKCE code. */
public class OAuthCallbackActivity extends AppCompatActivity {
    private boolean handling;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        handleCallback(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCallback(intent);
    }

    private void handleCallback(Intent intent) {
        if (handling) return;
        handling = true;
        Uri uri = intent == null ? null : intent.getData();
        if (uri == null || !"smkoin".equals(uri.getScheme()) || !"auth".equals(uri.getHost())
                || !"/callback".equals(uri.getPath())) {
            finishToApp("The Google callback was invalid.");
            return;
        }
        String error = uri.getQueryParameter("error_description");
        if (error == null) error = uri.getQueryParameter("error");
        if (error != null && !error.trim().isEmpty()) {
            SupabaseRepository.get(this).cancelGoogleIdentityLink();
            finishToApp("Google linking was cancelled or failed: " + error);
            return;
        }
        String code = uri.getQueryParameter("code");
        String returnedState = uri.getQueryParameter("state");
        SupabaseRepository.get(this).completeGoogleIdentityLink(code, returnedState, (ignored, failure) -> {
            if (isFinishing()) return;
            finishToApp(failure == null ? "Google account connected." : "Google account was not connected: " + failure.getMessage());
        });
    }

    private void finishToApp(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(main);
        finish();
    }
}
