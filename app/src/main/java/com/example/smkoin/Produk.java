package com.example.smkoin;

public class Produk {
    private String nama;
    private int hargaKoin;
    private int gambarResId;

    public Produk(String nama, int hargaKoin, int gambarResId) {
        this.nama = nama;
        this.hargaKoin = hargaKoin;
        this.gambarResId = gambarResId;
    }

    public String getNama() {
        return nama;
    }

    public int getHargaKoin() {
        return hargaKoin;
    }

    public int getGambarResId() {
        return gambarResId;
    }
}