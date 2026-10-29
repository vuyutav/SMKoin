package com.example.smkoin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MyOrderActivity extends AppCompatActivity {
    private static final int AUTH_REQUEST = 41;
    private String productId, productName, imageUrl;
    private int price, stock, quantity = 1;
    private TextView quantityValue, totalValue, message;
    private android.widget.Button submit;
    private EditText location;
    private boolean submitting;
    private boolean protectedUiReady;
    private boolean resumeSessionCheck;
    private SupabaseRepository repository;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthGate.requireSession(this, this::showOrder);
    }

    private void showOrder() {
        if (isFinishing()) return;
        protectedUiReady = true;
        repository = SupabaseRepository.get(this);
        productName = getIntent().getStringExtra("EXTRA_NAMA");
        if (productName == null) productName = "Product";
        productId = getIntent().getStringExtra("EXTRA_ID");
        imageUrl = getIntent().getStringExtra("EXTRA_IMAGE_URL");
        price = getIntent().getIntExtra("EXTRA_HARGA", 0);
        stock = getIntent().getIntExtra("EXTRA_STOCK", 0);
        int imageId = getIntent().getIntExtra("EXTRA_GAMBAR", 0);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(this);
        page.setPadding(AppUi.dp(this, 20), AppUi.dp(this, 18), AppUi.dp(this, 20), AppUi.dp(this, 28));
        scroll.addView(page);
        LinearLayout header = AppUi.row(this);
        TextView back = AppUi.text(this, "‹", 32, false, AppUi.INK);
        back.setContentDescription("Back");
        header.addView(back);
        header.addView(AppUi.text(this, "My Order", 22, true, AppUi.INK));
        back.setOnClickListener(v -> finish());
        page.addView(header);

        LinearLayout product = AppUi.card(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ProductImageLoader.load(image, imageUrl, imageId == 0 ? R.drawable.img : imageId);
        image.setContentDescription(productName + " image");
        product.addView(image, new LinearLayout.LayoutParams(-1, AppUi.dp(this, 220)));
        AppUi.space(this, product, 10);
        product.addView(AppUi.text(this, productName, 22, true, AppUi.INK));
        product.addView(AppUi.text(this, price + " pts", 14, false, AppUi.MUTED));
        LinearLayout.LayoutParams productParams = new LinearLayout.LayoutParams(-1, -2);
        productParams.topMargin = AppUi.dp(this, 14);
        page.addView(product, productParams);

        AppUi.space(this, page, 18);
        page.addView(AppUi.text(this, "Details", 20, true, AppUi.INK));
        AppUi.space(this, page, 8);
        LinearLayout details = AppUi.card(this);
        String weight = getIntent().getStringExtra("EXTRA_WEIGHT");
        details.addView(AppUi.text(this, "Weight / volume     " + (weight == null || weight.isEmpty() ? "Not provided" : weight), 14, false, AppUi.INK));
        AppUi.space(this, details, 10);
        LinearLayout countRow = AppUi.row(this);
        TextView minus = AppUi.text(this, "−", 24, true, AppUi.INK);
        minus.setGravity(Gravity.CENTER);
        TextView plus = AppUi.text(this, "+", 24, true, AppUi.INK);
        plus.setGravity(Gravity.CENTER);
        quantityValue = AppUi.text(this, "1", 16, true, AppUi.INK);
        quantityValue.setGravity(Gravity.CENTER);
        countRow.addView(AppUi.text(this, "Order quantity", 14, false, AppUi.INK), new LinearLayout.LayoutParams(0, -2, 1));
        countRow.addView(minus, new LinearLayout.LayoutParams(AppUi.dp(this, 40), AppUi.dp(this, 40)));
        countRow.addView(quantityValue, new LinearLayout.LayoutParams(AppUi.dp(this, 36), -2));
        countRow.addView(plus, new LinearLayout.LayoutParams(AppUi.dp(this, 40), AppUi.dp(this, 40)));
        details.addView(countRow);
        minus.setOnClickListener(v -> { if (quantity > 1) { quantity--; updateTotal(); } });
        plus.setOnClickListener(v -> { if (quantity < Math.min(stock, 100)) { quantity++; updateTotal(); } });
        AppUi.space(this, details, 8);
        location = new EditText(this);
        location.setSingleLine(true);
        location.setHint("Redemption location (optional)");
        location.setTextSize(14);
        location.setBackground(AppUi.background(AppUi.SOFT, AppUi.dp(this, 12), 0));
        location.setPadding(AppUi.dp(this, 12), 0, AppUi.dp(this, 12), 0);
        details.addView(location, new LinearLayout.LayoutParams(-1, AppUi.dp(this, 48)));
        page.addView(details);

        AppUi.space(this, page, 14);
        LinearLayout total = AppUi.card(this);
        total.addView(AppUi.text(this, "Total points", 13, false, AppUi.MUTED));
        totalValue = AppUi.text(this, "", 22, true, AppUi.INK);
        total.addView(totalValue);
        page.addView(total);
        AppUi.space(this, page, 10);
        message = AppUi.text(this, "Redemption is validated against current points and stock by Supabase.", 13, false, AppUi.MUTED);
        page.addView(message);
        AppUi.space(this, page, 10);
        boolean purchasable = repository.isConfigured() && productId != null && !productId.isEmpty() && stock > 0;
        submit = AppUi.button(this, purchasable ? "Confirm redemption" : "Redemption unavailable", true);
        submit.setEnabled(purchasable);
        submit.setAlpha(purchasable ? 1f : 0.5f);
        page.addView(submit, new LinearLayout.LayoutParams(-1, AppUi.dp(this, 52)));
        submit.setOnClickListener(v -> confirmRedemption());
        updateTotal();
        if (!purchasable) message.setText("Only active products with known stock from the connected catalog can be redeemed.");
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!protectedUiReady || resumeSessionCheck) return;
        resumeSessionCheck = true;
        SupabaseRepository.get(this).validateSession((valid, error) -> {
            if (isFinishing()) return;
            resumeSessionCheck = false;
            if (error != null || !Boolean.TRUE.equals(valid)) AuthGate.redirect(this, error);
        });
    }

    private void updateTotal() {
        if (quantityValue != null) quantityValue.setText(String.valueOf(quantity));
        if (totalValue != null) totalValue.setText(((long) price * quantity) + " pts");
    }

    private void confirmRedemption() {
        if (submitting) return;
        if (!repository.hasSession()) {
            Intent login = new Intent(this, AuthActivity.class);
            login.putExtra(AuthActivity.EXTRA_PROTECTED_CALLER, true);
            startActivityForResult(login, AUTH_REQUEST);
            return;
        }
        new AlertDialog.Builder(this).setTitle("Confirm redemption")
                .setMessage("Redeem " + quantity + " × " + productName + " for " + ((long) price * quantity) + " points?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) -> redeemNow()).show();
    }

    private void redeemNow() {
        if (submitting) return;
        submitting = true;
        submit.setEnabled(false);
        submit.setText("Submitting…");
        repository.redeem(productId, quantity, location.getText().toString(), (orderId, error) -> {
            if (isFinishing()) return;
            submitting = false;
            if (error != null) {
                if (error instanceof SupabaseRepository.AmbiguousRedemptionException) {
                    submit.setEnabled(false);
                    submit.setText("Outcome unclear");
                    message.setText(error.getMessage());
                } else {
                    submit.setEnabled(true);
                    submit.setText("Confirm redemption");
                    message.setText(error.getMessage());
                }
                return;
            }
            new AlertDialog.Builder(this).setTitle("Redemption confirmed")
                    .setMessage("Your order was accepted by SMKoin." + (orderId == null || orderId.isEmpty() ? "" : "\nOrder: " + orderId))
                    .setPositiveButton("Done", (dialog, which) -> finish()).setCancelable(false).show();
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST && resultCode == RESULT_OK) confirmRedemption();
    }
}
