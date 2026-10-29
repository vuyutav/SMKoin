package com.example.smkoin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {
    private TextView balanceValue, incomeValue, outcomeValue, emptyText;
    private LinearLayout entries;
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
        balanceValue = AppUi.text(requireContext(), "Unavailable", 30, true, AppUi.INK);
        balance.addView(balanceValue);
        balance.addView(AppUi.text(requireContext(), "Calculated from your transaction ledger.", 12, false, AppUi.MUTED));
        page.addView(balance);
        AppUi.space(requireContext(), page, 12);
        LinearLayout summary = AppUi.row(requireContext());
        summary.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 18), 0));
        summary.setPadding(AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16), AppUi.dp(requireContext(), 16));
        LinearLayout income = AppUi.column(requireContext());
        income.addView(AppUi.text(requireContext(), "Recent income", 14, false, AppUi.MUTED));
        incomeValue = AppUi.text(requireContext(), "— pts", 18, true, AppUi.INK);
        income.addView(incomeValue);
        LinearLayout outcome = AppUi.column(requireContext());
        outcome.addView(AppUi.text(requireContext(), "Recent outcome", 14, false, AppUi.MUTED));
        outcomeValue = AppUi.text(requireContext(), "— pts", 18, true, AppUi.INK);
        outcome.addView(outcomeValue);
        summary.addView(income, new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(outcome, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(summary);
        AppUi.space(requireContext(), page, 24);
        page.addView(AppUi.text(requireContext(), "Transactions", 19, true, AppUi.INK));
        AppUi.space(requireContext(), page, 10);
        emptyText = AppUi.text(requireContext(), "Sign in to view transactions.", 14, false, AppUi.MUTED);
        LinearLayout empty = AppUi.card(requireContext());
        empty.setGravity(Gravity.CENTER);
        empty.addView(emptyText);
        page.addView(empty);
        entries = AppUi.column(requireContext());
        LinearLayout.LayoutParams entriesParams = new LinearLayout.LayoutParams(-1, -2);
        entriesParams.topMargin = AppUi.dp(requireContext(), 10);
        page.addView(entries, entriesParams);
        loadTransactions();
        return scroll;
    }

    private void loadTransactions() {
        SupabaseRepository repository = SupabaseRepository.get(requireContext());
        if (!repository.isConfigured()) {
            balanceValue.setText("Unavailable");
            emptyText.setText("Preview mode · transaction service is not configured.");
            return;
        }
        if (!repository.hasSession()) {
            emptyText.setText("Sign in to view your balance and transaction history.");
            return;
        }
        emptyText.setText("Loading recent transactions…");
        repository.loadBalance((balance, error) -> {
            if (!isAdded()) return;
            if (error == null) balanceValue.setText(String.format(java.util.Locale.getDefault(), "%,d pts", balance));
            else emptyText.setText(error.getMessage());
        });
        repository.loadTransactions((transactions, error) -> {
            if (!isAdded()) return;
            if (error != null) { emptyText.setText(error.getMessage()); return; }
            entries.removeAllViews();
            long income = 0L, outcome = 0L;
            for (PointTransaction transaction : transactions) {
                if (transaction.pointsDelta > 0) income += transaction.pointsDelta;
                else outcome -= (long) transaction.pointsDelta;
                addTransaction(transaction);
            }
            incomeValue.setText(String.format(java.util.Locale.getDefault(), "%,d pts", income));
            outcomeValue.setText(String.format(java.util.Locale.getDefault(), "%,d pts", outcome));
            emptyText.setText(transactions.isEmpty() ? "No transactions yet." : "Showing the latest " + transactions.size() + " records.");
        });
    }

    private void addTransaction(PointTransaction transaction) {
        LinearLayout row = AppUi.row(requireContext());
        row.setPadding(AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 12), AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 12));
        row.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 14), 0));
        LinearLayout textBlock = AppUi.column(requireContext());
        String label = transaction.description.isEmpty() ? transaction.type.replace('_', ' ') : transaction.description;
        textBlock.addView(AppUi.text(requireContext(), label, 14, true, AppUi.INK));
        textBlock.addView(AppUi.text(requireContext(), transaction.createdAt.length() >= 10 ? transaction.createdAt.substring(0, 10) : transaction.createdAt, 12, false, AppUi.MUTED));
        row.addView(textBlock, new LinearLayout.LayoutParams(0, -2, 1));
        String amount = (transaction.pointsDelta > 0 ? "+" : "") + transaction.pointsDelta + " pts";
        row.addView(AppUi.text(requireContext(), amount, 14, true, transaction.pointsDelta > 0 ? 0xFF4A8B00 : AppUi.INK));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = AppUi.dp(requireContext(), 8);
        entries.addView(row, params);
    }
}
