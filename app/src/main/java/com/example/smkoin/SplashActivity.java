package com.example.smkoin; // Ganti dengan nama package Anda

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Mengatur waktu delay splash screen (3000 ms = 3 detik)
    private static final int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Menyembunyikan Action Bar di atas agar Splash Screen full screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Handler untuk menunda perpindahan ke MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Berpindah dari SplashActivity ke MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);

                // Menutup SplashActivity agar pengguna tidak bisa kembali ke layar ini dengan tombol "Back"
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}