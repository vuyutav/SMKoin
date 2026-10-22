package com.example.smkoin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 28));
        scroll.addView(page);
        page.addView(AppUi.text(requireContext(), "History", 24, true, AppUi.INK));
        AppUi.space(requireContext(), page, 16);
        LinearLayout balance = AppUi.card(requireContext());
        balance.addView(AppUi.text(requireContext(), "Your Point Balance", 14, true, AppUi.MUTED));
        balance.addView(AppUi.text(requireContext(), "Unavailable", 30, true, AppUi.INK));
        balance.addView(AppUi.text(requireContext(), "Account data is not connected.", 12, false, AppUi.MUTED));
        page.addView(balance);
        AppUi.space(requireContext(), page, 12);
        LinearLayout summary = AppUi.row(requireContext());
        summary.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 18), 0));
        summary.setPadding(AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16));
        LinearLayout income = AppUi.column(requireContext());
        income.addView(AppUi.text(requireContext(), "Income", 14, false, AppUi.MUTED));
        income.addView(AppUi.text(requireContext(), "— pts", 18, true, AppUi.INK));
        LinearLayout outcome = AppUi.column(requireContext());
        outcome.addView(AppUi.text(requireContext(), "Outcome", 14, false, AppUi.MUTED));
        outcome.addView(AppUi.text(requireContext(), "— pts", 18, true, AppUi.INK));
        summary.addView(income, new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(outcome, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(summary);
        AppUi.space(requireContext(), page, 24);
        page.addView(AppUi.text(requireContext(), "Transactions", 19, true, AppUi.INK));
        AppUi.space(requireContext(), page, 10);
        LinearLayout empty = AppUi.card(requireContext());
        empty.setGravity(Gravity.CENTER);
        empty.addView(AppUi.text(requireContext(), "No transaction data", 16, true, AppUi.INK));
        AppUi.space(requireContext(), empty, 6);
        empty.addView(AppUi.text(requireContext(), "Your earnings and redemptions will appear here when connected to the SMKoin service.", 14, false, AppUi.MUTED));
        page.addView(empty);
        return scroll;
    }
}
