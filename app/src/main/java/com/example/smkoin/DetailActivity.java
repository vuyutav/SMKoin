package com.example.smkoin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {
    private boolean protectedUiReady;
    private boolean resumeSessionCheck;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthGate.requireSession(this, this::showProduct);
    }

    private void showProduct() {
        if (isFinishing()) return;
        protectedUiReady = true;
        setContentView(R.layout.activity_detail);
        String name = getIntent().getStringExtra("EXTRA_NAMA");
        int price = getIntent().getIntExtra("EXTRA_HARGA", 0);
        int image = getIntent().getIntExtra("EXTRA_GAMBAR", 0);
        String category = getIntent().getStringExtra("EXTRA_CATEGORY");
        String productId = getIntent().getStringExtra("EXTRA_ID");
        String imageUrl = getIntent().getStringExtra("EXTRA_IMAGE_URL");
        String weight = getIntent().getStringExtra("EXTRA_WEIGHT");
        String productDescription = getIntent().getStringExtra("EXTRA_DESCRIPTION");
        int stock = getIntent().getIntExtra("EXTRA_STOCK", 0);
        TextView title = findViewById(R.id.tvDetailNama);
        TextView amount = findViewById(R.id.tvDetailHarga);
        TextView description = findViewById(R.id.tvDeskripsi);
        ImageView picture = findViewById(R.id.ivDetailGambar);
        title.setText(name == null ? "Product" : name);
        amount.setText(price + " pts");
        if (productId == null || productId.isEmpty()) {
            description.setText("Preview item · connect Supabase to view trusted product information and redeem rewards.");
        } else {
            String info = productDescription == null || productDescription.trim().isEmpty() ? "No description provided." : productDescription;
            String categoryInfo = category == null || category.isEmpty() ? "" : "\nCategory: " + category;
            String weightInfo = weight == null || weight.isEmpty() ? "" : "\nWeight / volume: " + weight;
            description.setText(info + categoryInfo + weightInfo + "\nAvailable: " + stock);
        }
        ProductImageLoader.load(picture, imageUrl, image == 0 ? R.drawable.img : image);
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        Button action = findViewById(R.id.btnTukarKoin);
        boolean liveProduct = productId != null && !productId.isEmpty();
        action.setText(liveProduct && stock > 0 ? "Review order" : "Unavailable for redemption");
        action.setEnabled(liveProduct && stock > 0);
        action.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyOrderActivity.class);
            intent.putExtra("EXTRA_NAMA", name);
            intent.putExtra("EXTRA_HARGA", price);
            intent.putExtra("EXTRA_GAMBAR", image);
            intent.putExtra("EXTRA_CATEGORY", category);
            intent.putExtra("EXTRA_ID", productId);
            intent.putExtra("EXTRA_DESCRIPTION", productDescription);
            intent.putExtra("EXTRA_IMAGE_URL", imageUrl);
            intent.putExtra("EXTRA_WEIGHT", weight);
            intent.putExtra("EXTRA_STOCK", stock);
            startActivity(intent);
        });
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
}
