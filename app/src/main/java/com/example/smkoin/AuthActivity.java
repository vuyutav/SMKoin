package com.example.smkoin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {
    static final String EXTRA_AUTH_ENTRY = "auth_entry";
    static final String EXTRA_PROTECTED_CALLER = "protected_caller";
    private boolean creatingAccount;
    private EditText nameField, emailField, passwordField, confirmPasswordField;
    private TextView title, submit, toggle, status;
    private SupabaseRepository repository;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = SupabaseRepository.get(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(this);
        page.setPadding(AppUi.dp(this, 22), AppUi.dp(this, 28), AppUi.dp(this, 22), AppUi.dp(this, 28));
        scroll.addView(page);
        TextView back = AppUi.text(this, "‹  Back", 16, true, AppUi.INK);
        back.setOnClickListener(v -> leaveAuthentication());
        page.addView(back);
        AppUi.space(this, page, 26);
        title = AppUi.text(this, "Sign in", 26, true, AppUi.INK);
        page.addView(title);
        page.addView(AppUi.text(this, "Use your SMKoin account.", 14, false, AppUi.MUTED));

        nameField = field("Full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        emailField = field("Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        passwordField = field("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordField = field("Confirm password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        page.addView(nameField, inputParams());
        page.addView(emailField, inputParams());
        page.addView(passwordField, inputParams());
        page.addView(confirmPasswordField, inputParams());
        submit = AppUi.text(this, "Sign in", 16, true, AppUi.INK);
        submit.setGravity(Gravity.CENTER);
        submit.setBackground(AppUi.background(AppUi.LIME, AppUi.dp(this, 16), 0));
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(-1, AppUi.dp(this, 52));
        submitParams.topMargin = AppUi.dp(this, 18);
        page.addView(submit, submitParams);
        submit.setOnClickListener(v -> submit());
        toggle = AppUi.text(this, "Create an account", 14, true, AppUi.INK);
        toggle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-1, AppUi.dp(this, 48));
        page.addView(toggle, toggleParams);
        toggle.setOnClickListener(v -> setMode(!creatingAccount));
        status = AppUi.text(this, "", 13, false, AppUi.MUTED);
        page.addView(status);
        setMode(false);
        if (!repository.isConfigured()) {
            status.setText("Supabase is not configured. Add the project URL and publishable key to your local .env file.");
            submit.setEnabled(false);
            submit.setAlpha(0.5f);
        }
        setContentView(scroll);
    }

    private EditText field(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setInputType(inputType);
        field.setTextSize(15);
        field.setPadding(AppUi.dp(this, 14), 0, AppUi.dp(this, 14), 0);
        field.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(this, 14), 0));
        return field;
    }

    private LinearLayout.LayoutParams inputParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, AppUi.dp(this, 52));
        params.topMargin = AppUi.dp(this, 12);
        return params;
    }

    private void setMode(boolean create) {
        creatingAccount = create;
        title.setText(create ? "Create account" : "Sign in");
        submit.setText(create ? "Create account" : "Sign in");
        toggle.setText(create ? "Already have an account? Sign in" : "Create an account");
        nameField.setVisibility(create ? android.view.View.VISIBLE : android.view.View.GONE);
        confirmPasswordField.setVisibility(create ? android.view.View.VISIBLE : android.view.View.GONE);
        status.setText("");
    }

    private void submit() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        String confirmation = confirmPasswordField.getText().toString();
        String name = nameField.getText().toString().trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            status.setText("Enter a valid email address.");
            return;
        }
        if (password.isEmpty() || (creatingAccount && name.isEmpty())) {
            status.setText("Complete the required fields.");
            return;
        }
        if (creatingAccount && !password.equals(confirmation)) {
            status.setText("The passwords do not match.");
            return;
        }
        submit.setEnabled(false);
        submit.setText(creatingAccount ? "Creating account…" : "Signing in…");
        SupabaseRepository.Callback<SupabaseRepository.UserProfile> callback = (profile, error) -> {
            if (isFinishing()) return;
            submit.setEnabled(true);
            submit.setText(creatingAccount ? "Create account" : "Sign in");
            if (error != null) status.setText(error.getMessage());
            else {
                setResult(RESULT_OK);
                if (getIntent().getBooleanExtra(EXTRA_AUTH_ENTRY, false)) {
                    Intent home = new Intent(this, MainActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(home);
                }
                finish();
            }
        };
        if (creatingAccount) repository.signUp(name, email, password, callback);
        else repository.signIn(email, password, callback);
    }

    private void leaveAuthentication() {
        if (getIntent().getBooleanExtra(EXTRA_PROTECTED_CALLER, false)) {
            // A protected screen launched Login after its session disappeared.
            // Do not leave that screen available behind Login if the user cancels.
            finishAffinity();
        } else {
            finish();
        }
    }

    @Override public void onBackPressed() {
        leaveAuthentication();
    }
}
