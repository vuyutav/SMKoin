package com.example.smkoin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        String name = getIntent().getStringExtra("EXTRA_NAMA");
        int price = getIntent().getIntExtra("EXTRA_HARGA", 0);
        int image = getIntent().getIntExtra("EXTRA_GAMBAR", 0);
        String category = getIntent().getStringExtra("EXTRA_CATEGORY");
        TextView title = findViewById(R.id.tvDetailNama);
        TextView amount = findViewById(R.id.tvDetailHarga);
        TextView description = findViewById(R.id.tvDeskripsi);
        ImageView picture = findViewById(R.id.ivDetailGambar);
        title.setText(name == null ? "Product" : name);
        amount.setText(price + " pts");
        description.setText("Preview catalog item" + (category == null ? "." : " in " + category + ".")
                + " Product description, stock, and redemption rules are not available until the SMKoin catalog is connected.");
        if (image != 0) picture.setImageResource(image);
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        Button action = findViewById(R.id.btnTukarKoin);
        action.setText("Review order");
        action.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyOrderActivity.class);
            intent.putExtra("EXTRA_NAMA", name);
            intent.putExtra("EXTRA_HARGA", price);
            intent.putExtra("EXTRA_GAMBAR", image);
            intent.putExtra("EXTRA_CATEGORY", category);
            startActivity(intent);
        });
    }
}
