package com.example.smkoin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PointShopFragment extends Fragment {
    private final List<Produk> products = SampleCatalog.products();
    private ProdukAdapter adapter;
    private String selectedCategory = "All";
    private String query = "";
    private TextView emptyState;

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
        balance.addView(AppUi.text(requireContext(), "Unavailable", 28, true, AppUi.INK));
        balance.addView(AppUi.text(requireContext(), "Account points are not connected.", 12, false, AppUi.MUTED));

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
        LinearLayout categories = AppUi.row(requireContext());
        String[] names = {"All", "Snacks", "Beverages", "Groceries", "Chilled"};
        for (String name : names) {
            TextView chip = AppUi.text(requireContext(), name, 13, true, AppUi.INK);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AppUi.dp(requireContext(), 13), AppUi.dp(requireContext(), 9), AppUi.dp(requireContext(), 13), AppUi.dp(requireContext(), 9));
            chip.setBackground(AppUi.background(name.equals(selectedCategory) ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 22), 0));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
            chipParams.rightMargin = AppUi.dp(requireContext(), 8);
            categories.addView(chip, chipParams);
            chip.setOnClickListener(v -> { selectedCategory = name; refreshChips(categories, names); filter(); });
        }
        categoryScroll.addView(categories);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(-1, -2);
        categoryParams.topMargin = AppUi.dp(requireContext(), 12);
        page.addView(categoryScroll, categoryParams);

        TextView demo = AppUi.text(requireContext(), "Preview catalog · items, prices, and availability are examples", 12, false, AppUi.MUTED);
        LinearLayout.LayoutParams demoParams = new LinearLayout.LayoutParams(-1, -2);
        demoParams.topMargin = AppUi.dp(requireContext(), 12);
        page.addView(demo, demoParams);
        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        list.setClipToPadding(false);
        list.setPadding(0, AppUi.dp(requireContext(), 8), 0, AppUi.dp(requireContext(), 12));
        adapter = new ProdukAdapter(products);
        list.setAdapter(adapter);
        page.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        emptyState = AppUi.text(requireContext(), "No products found.", 15, false, AppUi.MUTED);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setVisibility(View.GONE);
        page.addView(emptyState);
        return page;
    }

    private void refreshChips(LinearLayout chips, String[] names) {
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView chip = (TextView) chips.getChildAt(i);
            chip.setBackground(AppUi.background(names[i].equals(selectedCategory) ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 22), 0));
        }
    }

    private void filter() {
        if (adapter == null) return;
        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Produk> filtered = new ArrayList<>();
        for (Produk product : products) {
            boolean categoryMatch = selectedCategory.equals("All") || selectedCategory.equals(product.getCategory());
            boolean queryMatch = product.getNama().toLowerCase(Locale.ROOT).contains(needle);
            if (categoryMatch && queryMatch) filtered.add(product);
        }
        adapter.setProducts(filtered);
        if (emptyState != null) emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
