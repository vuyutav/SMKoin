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
    private final PointBalancePresenter balancePresenter = new PointBalancePresenter();
    private TextView balanceValue;
    private TextView balanceStatus;
    private TextView greeting;
    private TextView offerTitle;
    private TextView offerDescription;
    private android.widget.ImageView offerImage;
    private ProgressBar balanceLoading;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 24));
        scroll.addView(page);

        LinearLayout header = AppUi.row(requireContext());
        LinearLayout titleBlock = AppUi.column(requireContext());
        greeting = AppUi.text(requireContext(), "Welcome to SMKoin", 22, true, AppUi.INK);
        titleBlock.addView(greeting);
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
        balanceValue = AppUi.text(requireContext(), "Loading…", 28, true, AppUi.INK);
        amounts.addView(balanceValue);
        row.addView(amounts, new LinearLayout.LayoutParams(0, -2, 1));
        balanceLoading = new ProgressBar(requireContext());
        balanceLoading.setVisibility(View.GONE);
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
                AppUi.dp(requireContext(), 22), AppUi.dp(requireContext(), 22));
        loadingParams.rightMargin = AppUi.dp(requireContext(), 10);
        row.addView(balanceLoading, loadingParams);
        TextView visibility = AppUi.text(requireContext(), "◉", 22, true, AppUi.MUTED);
        visibility.setContentDescription("Toggle point balance visibility");
        row.addView(visibility);
        visibility.setOnClickListener(v -> { balanceHidden = !balanceHidden; renderBalance(); });
        balance.addView(row);
        AppUi.space(requireContext(), balance, 10);
        balanceStatus = AppUi.text(requireContext(), "Loading your point balance…", 13, false, AppUi.MUTED);
        balanceStatus.setContentDescription("Point balance status");
        balance.addView(balanceStatus);
        balanceStatus.setOnClickListener(v -> {
            if (balancePresenter.state().status == PointBalancePresenter.Status.ERROR) refreshBalance();
        });
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
        offersTitle.addView(AppUi.text(requireContext(), "Featured", 12, true, AppUi.MUTED));
        page.addView(offersTitle);
        AppUi.space(requireContext(), page, 10);
        LinearLayout offer = AppUi.card(requireContext());
        offer.setGravity(Gravity.CENTER);
        offerImage = new android.widget.ImageView(requireContext());
        offerImage.setImageResource(R.drawable.img);
        offerImage.setContentDescription("Special offer");
        offerImage.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        offer.addView(offerImage, new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 130)));
        offerTitle = AppUi.text(requireContext(), "Explore Point Shop", 18, true, AppUi.INK);
        offerDescription = AppUi.text(requireContext(), "Connect to see current offers.", 13, false, AppUi.MUTED);
        offer.addView(offerTitle);
        offer.addView(offerDescription);
        offer.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_shop));
        page.addView(offer);
        loadAccountData();
        return scroll;
    }

    @Override public void onResume() {
        super.onResume();
        refreshBalance();
    }

    private void loadAccountData() {
        SupabaseRepository repository = SupabaseRepository.get(requireContext());
        if (!repository.isConfigured()) {
            offerDescription.setText("Preview mode · no live offers or account data configured.");
            return;
        }
        if (repository.hasSession()) {
            repository.loadProfile((profile, error) -> {
                if (isAdded() && error == null && profile != null && !profile.fullName.trim().isEmpty()) {
                    greeting.setText("Hello, " + profile.fullName.trim());
                }
            });
        }
        repository.loadOffers((offers, error) -> {
            if (!isAdded()) return;
            if (error != null) offerDescription.setText("Offers could not be loaded.");
            else if (offers.isEmpty()) offerDescription.setText("There are no current offers.");
            else {
                Offer current = offers.get(0);
                offerTitle.setText(current.title);
                offerDescription.setText(current.description.isEmpty() ? "See this offer in the Point Shop." : current.description);
                ProductImageLoader.load(offerImage, current.imageUrl, R.drawable.img);
            }
        });
    }

    private void refreshBalance() {
        if (!isAdded() || balanceValue == null || balanceStatus == null) return;
        SupabaseRepository repository = SupabaseRepository.get(requireContext());
        if (!repository.isConfigured()) {
            balancePresenter.setError("Supabase is not configured.");
            renderBalance();
            return;
        }
        PointBalancePresenter.Request request = balancePresenter.begin(repository.currentUserId());
        renderBalance();
        if (request == null) return;
        repository.loadBalance((balance, error) -> {
            if (!isAdded() || balanceValue == null || balanceStatus == null) return;
            balancePresenter.complete(request, balance, error);
            renderBalance();
        });
    }

    private void renderBalance() {
        if (balanceValue == null || balanceStatus == null || balanceLoading == null) return;
        PointBalancePresenter.State state = balancePresenter.state();
        balanceLoading.setVisibility(state.status == PointBalancePresenter.Status.LOADING ? View.VISIBLE : View.GONE);
        switch (state.status) {
            case AVAILABLE:
                balanceValue.setText(balanceHidden ? "••••"
                        : String.format(java.util.Locale.getDefault(), "%,d pts", state.balance));
                balanceValue.setContentDescription(balanceHidden ? "Point balance hidden" : "Point balance");
                break;
            case LOADING:
                balanceValue.setText("Loading…");
                balanceValue.setContentDescription("Loading point balance");
                break;
            case SIGN_IN_REQUIRED:
                balanceValue.setText("Sign in required");
                balanceValue.setContentDescription("Sign in to view your point balance");
                break;
            case ERROR:
            default:
                balanceValue.setText("Unavailable");
                balanceValue.setContentDescription("Point balance unavailable");
                break;
        }
        balanceStatus.setText(state.message);
        boolean canRetry = state.status == PointBalancePresenter.Status.ERROR;
        balanceStatus.setClickable(canRetry);
        balanceStatus.setFocusable(canRetry);
        balanceStatus.setContentDescription(canRetry ? state.message + " Tap to retry." : "Point balance status");
    }

    @Override public void onDestroyView() {
        balanceValue = null;
        balanceStatus = null;
        balanceLoading = null;
        super.onDestroyView();
    }

    private TextView actionTile(String icon, String label) {
        TextView tile = AppUi.text(requireContext(), icon + "   " + label + "     ›", 15, true, AppUi.INK);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(AppUi.dp(requireContext(), 14), 0, AppUi.dp(requireContext(), 10), 0);
        tile.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 18), 0));
        return tile;
    }
}
