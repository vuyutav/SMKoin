package com.example.smkoin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PointShopFragment extends Fragment {
    private final List<Produk> products = new ArrayList<>();
    private ProdukAdapter adapter;
    private String selectedCategory = "All";
    private String query = "";
    private TextView emptyState, catalogStatus, balanceValue;
    private LinearLayout categories;
    private RecyclerView list;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 18), 0);
        page.setBackgroundColor(AppUi.SOFT);
        LinearLayout header = AppUi.row(requireContext());
        header.addView(AppUi.text(requireContext(), "Point Shop", 23, true, AppUi.INK), new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(AppUi.text(requireContext(), "◎", 24, true, AppUi.INK));
        page.addView(header);

        LinearLayout balance = AppUi.card(requireContext());
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(-1, -2);
        balanceParams.topMargin = AppUi.dp(requireContext(), 14);
        page.addView(balance, balanceParams);
        balance.addView(AppUi.text(requireContext(), "Your Point Balance", 14, true, AppUi.MUTED));
        balanceValue = AppUi.text(requireContext(), "Unavailable", 28, true, AppUi.INK);
        balance.addView(balanceValue);
        balance.addView(AppUi.text(requireContext(), "Point balance comes from your account ledger.", 12, false, AppUi.MUTED));

        EditText search = new EditText(requireContext());
        search.setSingleLine(true);
        search.setHint("Search products");
        search.setTextSize(14);
        search.setPadding(AppUi.dp(requireContext(), 14), 0, AppUi.dp(requireContext(), 14), 0);
        search.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 14), 0));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 48));
        searchParams.topMargin = AppUi.dp(requireContext(), 14);
        page.addView(search, searchParams);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); filter(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        HorizontalScrollView categoryScroll = new HorizontalScrollView(requireContext());
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categories = AppUi.row(requireContext());
        categoryScroll.addView(categories);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(-1, -2);
        categoryParams.topMargin = AppUi.dp(requireContext(), 12);
        page.addView(categoryScroll, categoryParams);
        renderCategories();

        catalogStatus = AppUi.text(requireContext(), "", 12, false, AppUi.MUTED);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = AppUi.dp(requireContext(), 10);
        page.addView(catalogStatus, statusParams);
        FrameLayout content = new FrameLayout(requireContext());
        list = new RecyclerView(requireContext());
        list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        list.setClipToPadding(false);
        list.setPadding(0, AppUi.dp(requireContext(), 8), 0, AppUi.dp(requireContext(), 12));
        adapter = new ProdukAdapter(products);
        list.setAdapter(adapter);
        content.addView(list, new FrameLayout.LayoutParams(-1, -1));
        emptyState = AppUi.text(requireContext(), "No products found.", 15, false, AppUi.MUTED);
        emptyState.setGravity(Gravity.CENTER);
        content.addView(emptyState, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, 0, 1);
        page.addView(content, contentParams);

        SupabaseRepository repository = SupabaseRepository.get(requireContext());
        if (!repository.isConfigured()) {
            products.addAll(SampleCatalog.products());
            catalogStatus.setText("Preview catalog · examples only; connect Supabase for live products.");
            adapter.setProducts(products);
            renderCategories();
            filter();
        } else {
            catalogStatus.setText("Loading products…");
            emptyState.setText("Loading products…");
            repository.loadProducts((value, error) -> {
                if (!isAdded()) return;
                if (error != null) {
                    catalogStatus.setText(error.getMessage());
                    emptyState.setText("Could not load products. Please try again later.");
                    emptyState.setVisibility(View.VISIBLE);
                    return;
                }
                products.clear();
                products.addAll(value);
                catalogStatus.setText(value.isEmpty() ? "No active products are available." : "");
                renderCategories();
                filter();
            });
        }
        if (repository.hasSession()) repository.loadBalance((value, error) -> {
            if (!isAdded()) return;
            if (error == null) balanceValue.setText(String.format(Locale.getDefault(), "%,d pts", value));
        });
        else balanceValue.setText("Sign in to view");
        return page;
    }

    private void renderCategories() {
        if (categories == null) return;
        categories.removeAllViews();
        Set<String> names = new LinkedHashSet<>();
        names.add("All");
        for (Produk product : products) if (product.getCategory() != null && !product.getCategory().isEmpty()) names.add(product.getCategory());
        if (!names.contains(selectedCategory)) selectedCategory = "All";
        for (String name : names) {
            TextView chip = AppUi.text(requireContext(), name, 13, true, AppUi.INK);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AppUi.dp(requireContext(), 13), AppUi.dp(requireContext(), 9), AppUi.dp(requireContext(), 13), AppUi.dp(requireContext(), 9));
            chip.setBackground(AppUi.background(name.equals(selectedCategory) ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 22), 0));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.rightMargin = AppUi.dp(requireContext(), 8);
            categories.addView(chip, params);
            chip.setOnClickListener(v -> { selectedCategory = name; renderCategories(); filter(); });
        }
    }

    private void filter() {
        if (adapter == null || emptyState == null) return;
        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Produk> filtered = new ArrayList<>();
        for (Produk product : products) {
            boolean categoryMatch = selectedCategory.equals("All") || selectedCategory.equalsIgnoreCase(product.getCategory());
            boolean queryMatch = product.getNama().toLowerCase(Locale.ROOT).contains(needle);
            if (categoryMatch && queryMatch) filtered.add(product);
        }
        adapter.setProducts(filtered);
        if (filtered.isEmpty() && !products.isEmpty()) emptyState.setText("No products found.");
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
