package com.example.smkoin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 26));
        scroll.addView(page);
        page.addView(AppUi.text(requireContext(), "Account", 24, true, AppUi.INK));
        AppUi.space(requireContext(), page, 14);
        LinearLayout profile = AppUi.card(requireContext());
        profile.addView(AppUi.text(requireContext(), "◉   SMKoin account", 18, true, AppUi.INK));
        profile.addView(AppUi.text(requireContext(), "Sign-in and profile data are not connected.", 13, false, AppUi.MUTED));
        page.addView(profile);
        AppUi.space(requireContext(), page, 12);
        LinearLayout summary = AppUi.card(requireContext());
        summary.addView(AppUi.text(requireContext(), "Your coupon", 15, true, AppUi.INK));
        summary.addView(AppUi.text(requireContext(), "Coupons unavailable", 13, false, AppUi.MUTED));
        AppUi.space(requireContext(), summary, 10);
        summary.addView(AppUi.text(requireContext(), "Your credit", 15, true, AppUi.INK));
        summary.addView(AppUi.text(requireContext(), "Points unavailable", 13, false, AppUi.MUTED));
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
            item.setOnClickListener(v -> showUnavailable(option));
        }
        return scroll;
    }

    private void showUnavailable(String action) {
        boolean destructive = action.equals("Delete account") || action.equals("Log out");
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(action)
                .setMessage(destructive
                        ? "There is no signed-in account to change in this build."
                        : "This feature needs the SMKoin account service, which is not configured in this repository.")
                .setPositiveButton("OK", (d, which) -> { });
        if (action.equals("Delete account")) dialog.setNegativeButton("Cancel", (d, which) -> d.dismiss());
        dialog.show();
    }
}
