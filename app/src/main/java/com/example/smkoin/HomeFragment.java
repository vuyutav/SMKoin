package com.example.smkoin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private boolean balanceHidden;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 24));
        scroll.addView(page);

        LinearLayout header = AppUi.row(requireContext());
        LinearLayout titleBlock = AppUi.column(requireContext());
        titleBlock.addView(AppUi.text(requireContext(), "Good morning", 22, true, AppUi.INK));
        titleBlock.addView(AppUi.text(requireContext(), "Welcome to SMKoin", 14, false, AppUi.MUTED));
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1));
        TextView notification = AppUi.text(requireContext(), "♧", 24, true, AppUi.INK);
        notification.setContentDescription("Notifications");
        header.addView(notification);
        page.addView(header);

        AppUi.space(requireContext(), page, 20);
        LinearLayout balance = AppUi.card(requireContext());
        LinearLayout row = AppUi.row(requireContext());
        LinearLayout amounts = AppUi.column(requireContext());
        amounts.addView(AppUi.text(requireContext(), "Your Point Balance", 14, true, AppUi.MUTED));
        TextView value = AppUi.text(requireContext(), "Unavailable", 28, true, AppUi.INK);
        amounts.addView(value);
        row.addView(amounts, new LinearLayout.LayoutParams(0, -2, 1));
        TextView visibility = AppUi.text(requireContext(), "◉", 22, true, AppUi.MUTED);
        visibility.setContentDescription("Toggle point balance visibility");
        row.addView(visibility);
        visibility.setOnClickListener(v -> { balanceHidden = !balanceHidden; value.setText(balanceHidden ? "••••" : "Unavailable"); });
        balance.addView(row);
        AppUi.space(requireContext(), balance, 10);
        balance.addView(AppUi.text(requireContext(), "Connect an account to view your points.", 13, false, AppUi.MUTED));
        ProgressBar progress = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgress(0);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(AppUi.LIME));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 6));
        progressParams.topMargin = AppUi.dp(requireContext(), 12);
        balance.addView(progress, progressParams);
        page.addView(balance);

        AppUi.space(requireContext(), page, 22);
        page.addView(AppUi.text(requireContext(), "Quick Actions", 18, true, AppUi.INK));
        LinearLayout actions = AppUi.row(requireContext());
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView scan = actionTile("▣", "Scan & Earn");
        TextView settings = actionTile("⚙", "Settings");
        actions.addView(scan, new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 78), 1));
        LinearLayout.LayoutParams settingParams = new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 78), 1);
        settingParams.leftMargin = AppUi.dp(requireContext(), 12);
        actions.addView(settings, settingParams);
        scan.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_scan));
        settings.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_profile));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = AppUi.dp(requireContext(), 12);
        page.addView(actions, actionParams);

        AppUi.space(requireContext(), page, 22);
        LinearLayout offersTitle = AppUi.row(requireContext());
        offersTitle.addView(AppUi.text(requireContext(), "Special Offers", 18, true, AppUi.INK), new LinearLayout.LayoutParams(0, -2, 1));
        offersTitle.addView(AppUi.text(requireContext(), "Preview", 12, true, AppUi.MUTED));
        page.addView(offersTitle);
        AppUi.space(requireContext(), page, 10);
        LinearLayout offer = AppUi.card(requireContext());
        offer.setGravity(Gravity.CENTER);
        android.widget.ImageView image = new android.widget.ImageView(requireContext());
        image.setImageResource(R.drawable.img);
        image.setContentDescription("SMKoin rewards illustration");
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        offer.addView(image, new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 130)));
        offer.addView(AppUi.text(requireContext(), "Explore Point Shop", 18, true, AppUi.INK));
        offer.addView(AppUi.text(requireContext(), "Sample catalog · purchases unavailable", 13, false, AppUi.MUTED));
        offer.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_shop));
        page.addView(offer);
        return scroll;
    }

    private TextView actionTile(String icon, String label) {
        TextView tile = AppUi.text(requireContext(), icon + "   " + label + "     ›", 15, true, AppUi.INK);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(AppUi.dp(requireContext(), 14), 0, AppUi.dp(requireContext(), 10), 0);
        tile.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 18), 0));
        return tile;
    }
}
