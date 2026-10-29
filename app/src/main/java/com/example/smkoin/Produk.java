package com.example.smkoin;

public class Produk {
    private String id;
    private String nama;
    private int hargaKoin;
    private int gambarResId;
    private String category;
    private String description;
    private String imageUrl;
    private String weightLabel;
    private int stockQuantity;

    public Produk(String nama, int hargaKoin, int gambarResId) {
        this(nama, hargaKoin, gambarResId, "All");
    }

    public Produk(String nama, int hargaKoin, int gambarResId, String category) {
        this("", nama, hargaKoin, "", "", "", 0, category);
        this.gambarResId = gambarResId;
    }

    public Produk(String id, String nama, int hargaKoin, String description, String imageUrl,
                  String weightLabel, int stockQuantity, String category) {
        this.id = id;
        this.nama = nama;
        this.hargaKoin = hargaKoin;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.weightLabel = weightLabel;
        this.stockQuantity = stockQuantity;
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
    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getWeightLabel() { return weightLabel; }
    public int getStockQuantity() { return stockQuantity; }
}
