package com.example.smkoin;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MyOrderActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra("EXTRA_NAMA");
        if (name == null) name = "Product";
        final String productName = name;
        int price = getIntent().getIntExtra("EXTRA_HARGA", 0);
        int imageId = getIntent().getIntExtra("EXTRA_GAMBAR", 0);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(this);
        page.setPadding(AppUi.dp(this, 20), AppUi.dp(this, 18), AppUi.dp(this, 20), AppUi.dp(this, 28));
        scroll.addView(page);
        LinearLayout header = AppUi.row(this);
        android.widget.TextView back = AppUi.text(this, "‹", 32, false, AppUi.INK);
        header.addView(back);
        header.addView(AppUi.text(this, "My Order", 22, true, AppUi.INK));
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        page.addView(header);

        LinearLayout product = AppUi.card(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageResource(imageId == 0 ? R.drawable.img : imageId);
        image.setContentDescription("Preview product image");
        product.addView(image, new LinearLayout.LayoutParams(-1, AppUi.dp(this, 220)));
        AppUi.space(this, product, 10);
        product.addView(AppUi.text(this, productName, 22, true, AppUi.INK));
        product.addView(AppUi.text(this, price + " pts · preview price", 14, false, AppUi.MUTED));
        LinearLayout.LayoutParams productParams = new LinearLayout.LayoutParams(-1, -2);
        productParams.topMargin = AppUi.dp(this, 14);
        page.addView(product, productParams);

        AppUi.space(this, page, 18);
        page.addView(AppUi.text(this, "Details", 20, true, AppUi.INK));
        AppUi.space(this, page, 8);
        LinearLayout details = AppUi.card(this);
        details.addView(AppUi.text(this, "Weight / volume     Not provided", 14, false, AppUi.INK));
        AppUi.space(this, details, 10);
        details.addView(AppUi.text(this, "Order quantity     1", 14, false, AppUi.INK));
        AppUi.space(this, details, 10);
        details.addView(AppUi.text(this, "Redemption location     Not provided", 14, false, AppUi.INK));
        page.addView(details);

        AppUi.space(this, page, 16);
        LinearLayout total = AppUi.card(this);
        total.addView(AppUi.text(this, "Total · quantity 1", 13, false, AppUi.MUTED));
        total.addView(AppUi.text(this, price + " pts", 22, true, AppUi.INK));
        page.addView(total);
        AppUi.space(this, page, 12);
        android.widget.Button confirm = AppUi.button(this, "Confirm redemption", true);
        confirm.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Redemption unavailable")
                .setMessage("This preview catalog is not connected to an account, balance, or stock service. No order was placed.")
                .setPositiveButton("OK", (dialog, which) -> { }).show());
        page.addView(confirm, new LinearLayout.LayoutParams(-1, AppUi.dp(this, 52)));
        setContentView(scroll);
    }
}
