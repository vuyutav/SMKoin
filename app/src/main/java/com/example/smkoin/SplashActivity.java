package com.example.smkoin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {
    private MaterialButton retryButton;
    private TextView description;
    private boolean checking;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        retryButton = findViewById(R.id.btnContinue);
        description = findViewById(R.id.tvDescription);
        retryButton.setVisibility(View.GONE);
        checkSession();
    }

    private void checkSession() {
        if (checking || isFinishing()) return;
        checking = true;
        description.setText("Checking your SMKoin session…");
        SupabaseRepository.get(this).validateSession((valid, error) -> {
            if (isFinishing()) return;
            checking = false;
            if (error == null && Boolean.TRUE.equals(valid)) {
                Intent home = new Intent(this, MainActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
                finish();
            } else if (error == null) {
                openLogin();
            } else {
                description.setText("We couldn’t verify your session. Check your connection and try again.");
                retryButton.setText("Retry");
                retryButton.setVisibility(View.VISIBLE);
                retryButton.setOnClickListener(view -> {
                    retryButton.setVisibility(View.GONE);
                    checkSession();
                });
            }
        });
    }

    private void openLogin() {
        Intent login = new Intent(this, AuthActivity.class);
        login.putExtra(AuthActivity.EXTRA_AUTH_ENTRY, true);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        finish();
    }
}
