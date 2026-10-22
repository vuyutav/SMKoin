package com.example.smkoin;

public class Produk {
    private String nama;
    private int hargaKoin;
    private int gambarResId;
    private String category;

    public Produk(String nama, int hargaKoin, int gambarResId) {
        this(nama, hargaKoin, gambarResId, "All");
    }

    public Produk(String nama, int hargaKoin, int gambarResId, String category) {
        this.nama = nama;
        this.hargaKoin = hargaKoin;
        this.gambarResId = gambarResId;
        this.category = category;
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

    public String getCategory() { return category; }
}
