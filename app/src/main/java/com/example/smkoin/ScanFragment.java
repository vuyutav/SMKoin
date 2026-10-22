package com.example.smkoin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class ScanFragment extends Fragment {
    private String mode = "Receipt";
    private TextView receiptTab, qrTab;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 24));
        scroll.addView(page);
        page.addView(AppUi.text(requireContext(), "Scan & Earn", 24, true, AppUi.INK));
        page.addView(AppUi.text(requireContext(), "Scan a receipt or QR code to earn points", 14, false, AppUi.MUTED));
        LinearLayout modes = AppUi.row(requireContext());
        receiptTab = modeButton("▤  Receipt"); qrTab = modeButton("▦  QR Code");
        modes.addView(receiptTab, new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 52), 1));
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 52), 1);
        qrParams.leftMargin = AppUi.dp(requireContext(), 10);
        modes.addView(qrTab, qrParams);
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(-1, -2);
        modesParams.topMargin = AppUi.dp(requireContext(), 18);
        page.addView(modes, modesParams);
        receiptTab.setOnClickListener(v -> selectMode("Receipt"));
        qrTab.setOnClickListener(v -> selectMode("QR Code"));

        LinearLayout preview = AppUi.column(requireContext());
        preview.setGravity(Gravity.CENTER);
        preview.setBackground(AppUi.background(0xFFE8EAE4, AppUi.dp(requireContext(), 24), 0));
        TextView cameraIcon = AppUi.text(requireContext(), "▣", 52, true, AppUi.MUTED);
        cameraIcon.setGravity(Gravity.CENTER);
        preview.addView(cameraIcon);
        TextView stateText = AppUi.text(requireContext(), "Camera preview unavailable", 17, true, AppUi.INK);
        stateText.setGravity(Gravity.CENTER);
        preview.addView(stateText);
        TextView guidance = AppUi.text(requireContext(), "A camera scanner has not been configured for this app.", 14, false, AppUi.MUTED);
        guidance.setGravity(Gravity.CENTER);
        preview.addView(guidance);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 310));
        previewParams.topMargin = AppUi.dp(requireContext(), 18);
        page.addView(preview, previewParams);
        AppUi.space(requireContext(), page, 12);
        page.addView(AppUi.text(requireContext(), "Points are awarded only after a receipt or code is verified by the SMKoin service.", 13, false, AppUi.MUTED));
        AppUi.space(requireContext(), page, 14);
        android.widget.Button scan = AppUi.button(requireContext(), "Scan", true);
        page.addView(scan, new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 52)));
        scan.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Scanning unavailable")
                .setMessage("This repository has no camera scanner or receipt verification service. No points have been added.")
                .setPositiveButton("OK", (dialog, which) -> { }).show());
        selectMode(mode);
        return scroll;
    }

    private TextView modeButton(String label) {
        TextView button = AppUi.text(requireContext(), label, 14, true, AppUi.INK);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    private void selectMode(String value) {
        mode = value;
        if (receiptTab != null) receiptTab.setBackground(AppUi.background(mode.equals("Receipt") ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 16), 0));
        if (qrTab != null) qrTab.setBackground(AppUi.background(mode.equals("QR Code") ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 16), 0));
    }
}
