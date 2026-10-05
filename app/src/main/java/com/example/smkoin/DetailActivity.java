package com.example.smkoin; // Pastikan package sesuai

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    private ImageView ivGambar, ivBack;
    private TextView tvNama, tvHarga;
    private Button btnTukarKoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Sembunyikan Action Bar bawaan
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Hubungkan variabel dengan XML
        ivGambar = findViewById(R.id.ivDetailGambar);
        ivBack = findViewById(R.id.ivBack);
        tvNama = findViewById(R.id.tvDetailNama);
        tvHarga = findViewById(R.id.tvDetailHarga);
        btnTukarKoin = findViewById(R.id.btnTukarKoin);

        // Menangkap data (Intent) yang dikirim dari ProdukAdapter
        String namaProduk = getIntent().getStringExtra("EXTRA_NAMA");
        int hargaProduk = getIntent().getIntExtra("EXTRA_HARGA", 0);
        int gambarProduk = getIntent().getIntExtra("EXTRA_GAMBAR", 0);

        // Menampilkan data ke UI
        tvNama.setText(namaProduk);
        tvHarga.setText(hargaProduk + " Koin");
        if (gambarProduk != 0) {
            ivGambar.setImageResource(gambarProduk);
        }

        // Aksi Tombol Kembali (Back)
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Menutup halaman detail dan kembali ke Home
            }
        });

        btnTukarKoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Simpan kalimat riwayat ke dalam memori
                String riwayatBaru = "Membeli " + namaProduk + " seharga " + hargaProduk + " Koin";
                DataRiwayat.listRiwayat.add(riwayatBaru);

                // 2. Tampilkan notifikasi
                Toast.makeText(DetailActivity.this, "Berhasil menukar koin!", Toast.LENGTH_SHORT).show();

                // 3. Kembali ke beranda secara otomatis
                finish();
            }
        });
    }
}