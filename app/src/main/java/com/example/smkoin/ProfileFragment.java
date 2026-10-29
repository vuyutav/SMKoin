package com.example.smkoin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private TextView profileName, profileStatus, authAction, accountBalance;
    private SupabaseRepository repository;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        repository = SupabaseRepository.get(requireContext());
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 26));
        scroll.addView(page);
        page.addView(AppUi.text(requireContext(), "Account", 24, true, AppUi.INK));
        AppUi.space(requireContext(), page, 14);
        LinearLayout profile = AppUi.card(requireContext());
        profileName = AppUi.text(requireContext(), "SMKoin account", 18, true, AppUi.INK);
        profileStatus = AppUi.text(requireContext(), "Checking account…", 13, false, AppUi.MUTED);
        profile.addView(profileName);
        profile.addView(profileStatus);
        profile.setOnClickListener(v -> openAuth());
        page.addView(profile);
        authAction = AppUi.text(requireContext(), "Sign in / Create account", 15, true, AppUi.INK);
        authAction.setGravity(android.view.Gravity.CENTER);
        authAction.setBackground(AppUi.background(AppUi.LIME, AppUi.dp(requireContext(), 14), 0));
        LinearLayout.LayoutParams authParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 48));
        authParams.topMargin = AppUi.dp(requireContext(), 10);
        page.addView(authAction, authParams);
        authAction.setOnClickListener(v -> { if (repository.hasSession()) confirmSignOut(); else openAuth(); });

        AppUi.space(requireContext(), page, 12);
        LinearLayout summary = AppUi.card(requireContext());
        summary.addView(AppUi.text(requireContext(), "Your coupon", 15, true, AppUi.INK));
        summary.addView(AppUi.text(requireContext(), "Coupon list is not connected yet.", 13, false, AppUi.MUTED));
        AppUi.space(requireContext(), summary, 10);
        summary.addView(AppUi.text(requireContext(), "Your credit", 15, true, AppUi.INK));
        accountBalance = AppUi.text(requireContext(), "Points unavailable", 13, false, AppUi.MUTED);
        summary.addView(accountBalance);
        page.addView(summary);

        AppUi.space(requireContext(), page, 20);
        page.addView(AppUi.text(requireContext(), "Account Settings", 19, true, AppUi.INK));
        AppUi.space(requireContext(), page, 10);
        String[] options = {"Personal information", "Change password", "Login and security", "Connected accounts", "Delete account", "Log out"};
        for (String option : options) {
            LinearLayout item = AppUi.row(requireContext());
            item.setPadding(AppUi.dp(requireContext(), 15), AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 15), AppUi.dp(requireContext(), 14));
            item.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 14), 0));
            item.addView(AppUi.text(requireContext(), option, 15, true, option.equals("Delete account") ? 0xFFD32F2F : AppUi.INK), new LinearLayout.LayoutParams(0, -2, 1));
            item.addView(AppUi.text(requireContext(), "›", 22, false, AppUi.MUTED));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
            itemParams.bottomMargin = AppUi.dp(requireContext(), 8);
            page.addView(item, itemParams);
            item.setOnClickListener(v -> handleOption(option));
        }
        return scroll;
    }

    @Override public void onResume() {
        super.onResume();
        if (repository != null && profileName != null) refreshAccount();
    }

    private void refreshAccount() {
        if (!repository.isConfigured()) {
            profileName.setText("SMKoin account");
            profileStatus.setText("Supabase is not configured.");
            authAction.setText("Configuration unavailable");
            authAction.setEnabled(false);
            accountBalance.setText("Points unavailable");
            return;
        }
        authAction.setEnabled(true);
        if (!repository.hasSession()) {
            profileName.setText("Guest");
            profileStatus.setText("Sign in to access your profile and points.");
            authAction.setText("Sign in / Create account");
            accountBalance.setText("Sign in to view");
            return;
        }
        authAction.setText("Sign out");
        SupabaseRepository.UserProfile cached = repository.cachedUser();
        profileName.setText(cached == null || cached.fullName.isEmpty() ? "SMKoin member" : cached.fullName);
        profileStatus.setText(cached == null ? "Loading profile…" : cached.email);
        repository.loadProfile((profile, error) -> {
            if (!isAdded()) return;
            if (error == null && profile != null) {
                profileName.setText(profile.fullName.isEmpty() ? "SMKoin member" : profile.fullName);
                profileStatus.setText(profile.email);
            } else if (error != null) profileStatus.setText(error.getMessage());
        });
        repository.loadBalance((balance, error) -> {
            if (!isAdded()) return;
            accountBalance.setText(error == null
                    ? String.format(java.util.Locale.getDefault(), "%,d pts", balance)
                    : "Balance unavailable");
        });
    }

    private void openAuth() {
        Intent login = new Intent(requireContext(), AuthActivity.class);
        login.putExtra(AuthActivity.EXTRA_AUTH_ENTRY, true);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
    }

    private void handleOption(String option) {
        if (option.equals("Log out")) { confirmSignOut(); return; }
        if (!repository.hasSession()) { openAuth(); return; }
        if (option.equals("Personal information")) { editPersonalInformation(); return; }
        if (option.equals("Change password")) { changePassword(); return; }
        if (option.equals("Login and security")) {
            new AlertDialog.Builder(requireContext()).setTitle(option)
                    .setMessage("Your account is authenticated with Supabase email and password. Password changes require your current password to reauthenticate.")
                    .setPositiveButton("Change password", (dialog, which) -> changePassword())
                    .setNegativeButton("Close", null).show();
            return;
        }
        if (option.equals("Connected accounts")) { showGoogleAccountStatus(); return; }
        String message = option.equals("Delete account")
                ? "Account deletion removes your profile, point history, orders, coupons, receipt records, and receipt images. You will be asked to reauthenticate before continuing."
                : "This action is not implemented.";
        if (option.equals("Delete account")) {
            new AlertDialog.Builder(requireContext()).setTitle("Delete account permanently?")
                    .setMessage(message).setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue", (dialog, which) -> confirmDeleteAccount()).show();
        } else new AlertDialog.Builder(requireContext()).setTitle(option).setMessage(message).setPositiveButton("OK", null).show();
    }

    private void showGoogleAccountStatus() {
        repository.loadGoogleIdentityStatus((linked, error) -> {
            if (!isAdded()) return;
            if (error != null) {
                new AlertDialog.Builder(requireContext()).setTitle("Connected accounts")
                        .setMessage("Could not check the current Google connection: " + error.getMessage())
                        .setPositiveButton("OK", null).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()).setTitle("Google account")
                    .setMessage(Boolean.TRUE.equals(linked) ? "Google is connected to this SMKoin account." : "Google is not connected to this SMKoin account.");
            if (!Boolean.TRUE.equals(linked)) builder.setPositiveButton("Connect Google", (dialog, which) -> startGoogleLink());
            else builder.setPositiveButton("Done", null);
            builder.setNegativeButton("Close", null).show();
        });
    }

    private void startGoogleLink() {
        repository.startGoogleIdentityLink((url, error) -> {
            if (!isAdded()) return;
            if (error != null || url == null) {
                new AlertDialog.Builder(requireContext()).setTitle("Google linking unavailable")
                        .setMessage(error == null ? "Supabase did not return an authorization link." : error.getMessage())
                        .setPositiveButton("OK", null).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception noBrowser) {
                repository.cancelGoogleIdentityLink();
                Toast.makeText(requireContext(), "No browser is available to connect Google.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteAccount() {
        EditText password = passwordField("Current password");
        LinearLayout content = AppUi.column(requireContext());
        content.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 4), AppUi.dp(requireContext(), 20), 0);
        content.addView(password, fieldParams());
        TextView status = AppUi.text(requireContext(), "Reauthentication is required. This action cannot be undone.", 13, false, AppUi.MUTED);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = AppUi.dp(requireContext(), 10);
        content.addView(status, messageParams);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("Confirm account deletion")
                .setView(content).setNegativeButton("Cancel", null).setPositiveButton("Delete account", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String currentPassword = password.getText().toString();
            if (currentPassword.isEmpty()) { password.setError("Enter your current password."); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            password.setEnabled(false);
            status.setText("Verifying and deleting the account...");
            repository.deleteAccount(currentPassword, (ignoredResult, error) -> {
                if (!isAdded()) return;
                if (error != null) {
                    password.setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    status.setText("Deletion was not confirmed: " + error.getMessage());
                    return;
                }
                dialog.dismiss();
                Intent login = new Intent(requireContext(), AuthActivity.class);
                login.putExtra(AuthActivity.EXTRA_AUTH_ENTRY, true);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(login);
            });
        }));
        dialog.show();
    }

    private void editPersonalInformation() {
        repository.loadProfile((profile, error) -> {
            if (!isAdded()) return;
            if (error != null || profile == null) {
                new AlertDialog.Builder(requireContext()).setTitle("Personal information")
                        .setMessage(error == null ? "Profile unavailable." : error.getMessage())
                        .setPositiveButton("OK", null).show();
                return;
            }
            EditText nameInput = new EditText(requireContext());
            nameInput.setSingleLine(true);
            nameInput.setText(profile.fullName);
            nameInput.setHint("Full name");
            nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            nameInput.setPadding(AppUi.dp(requireContext(), 14), 0, AppUi.dp(requireContext(), 14), 0);
            nameInput.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 12), 0));
            LinearLayout content = AppUi.column(requireContext());
            content.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 4), AppUi.dp(requireContext(), 20), 0);
            content.addView(nameInput, new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 52)));
            TextView email = AppUi.text(requireContext(), profile.email, 14, false, AppUi.MUTED);
            LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(-1, -2);
            emailParams.topMargin = AppUi.dp(requireContext(), 12);
            content.addView(email, emailParams);
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("Personal information")
                    .setView(content).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String name = nameInput.getText().toString().trim();
                String validationError = AccountInputValidator.fullNameError(name);
                if (validationError != null) { nameInput.setError(validationError); return; }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                nameInput.setEnabled(false);
                repository.updateFullName(name, (updated, updateError) -> {
                    if (!isAdded()) return;
                    if (updateError != null || updated == null) {
                        nameInput.setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        nameInput.setError(updateError == null ? "Profile update was not confirmed." : updateError.getMessage());
                    } else {
                        profileName.setText(updated.fullName.isEmpty() ? "SMKoin member" : updated.fullName);
                        profileStatus.setText(updated.email);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
                    }
                });
            }));
            dialog.show();
        });
    }

    private void changePassword() {
        LinearLayout content = AppUi.column(requireContext());
        content.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 4), AppUi.dp(requireContext(), 20), 0);
        EditText current = passwordField("Current password");
        EditText next = passwordField("New password (8+ characters)");
        EditText confirm = passwordField("Confirm new password");
        TextView status = AppUi.text(requireContext(), "Your current password is used only to reauthenticate with Supabase.", 13, false, AppUi.MUTED);
        content.addView(current, fieldParams());
        content.addView(next, fieldParams());
        content.addView(confirm, fieldParams());
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = AppUi.dp(requireContext(), 10);
        content.addView(status, statusParams);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("Change password")
                .setView(content).setNegativeButton("Cancel", null).setPositiveButton("Update password", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String oldPassword = current.getText().toString();
            String newPassword = next.getText().toString();
            if (oldPassword.isEmpty()) { current.setError("Enter your current password."); return; }
            String passwordError = AccountInputValidator.newPasswordError(oldPassword, newPassword);
            if (passwordError != null) { next.setError(passwordError); return; }
            String confirmationError = AccountInputValidator.passwordConfirmationError(newPassword, confirm.getText().toString());
            if (confirmationError != null) { confirm.setError(confirmationError); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            current.setEnabled(false); next.setEnabled(false); confirm.setEnabled(false);
            status.setText("Verifying your account and updating the password...");
            repository.changePassword(oldPassword, newPassword, (ignoredResult, error) -> {
                if (!isAdded()) return;
                if (error != null) {
                    current.setEnabled(true); next.setEnabled(true); confirm.setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    status.setText(error.getMessage());
                } else {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Password changed successfully.", Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private EditText passwordField(String hint) {
        EditText field = new EditText(requireContext());
        field.setHint(hint);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setPadding(AppUi.dp(requireContext(), 14), 0, AppUi.dp(requireContext(), 14), 0);
        field.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 12), 0));
        return field;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 52));
        params.topMargin = AppUi.dp(requireContext(), 10);
        return params;
    }

    private void confirmSignOut() {
        if (!repository.hasSession()) { openAuth(); return; }
        new AlertDialog.Builder(requireContext()).setTitle("Log out?").setMessage("You will need to sign in again to view account data.")
                .setNegativeButton("Cancel", null).setPositiveButton("Log out", (dialog, which) -> {
                    repository.signOut((ignored, error) -> { /* Local logout is complete regardless of remote revocation. */ });
                    Intent login = new Intent(requireContext(), AuthActivity.class);
                    login.putExtra(AuthActivity.EXTRA_AUTH_ENTRY, true);
                    login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(login);
                }).show();
    }
}
