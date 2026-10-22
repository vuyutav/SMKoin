package com.example.smkoin;

import java.util.ArrayList;
import java.util.List;

/** Preview-only items retained from the original sample catalog; not purchasable inventory. */
final class SampleCatalog {
    private SampleCatalog() { }
    static List<Produk> products() {
        List<Produk> items = new ArrayList<>();
        items.add(new Produk("Chocolate Bun", 250, R.drawable.img, "Snacks"));
        items.add(new Produk("Iced Tea", 100, R.drawable.img, "Beverages"));
        items.add(new Produk("Notebook", 400, R.drawable.img, "Groceries"));
        items.add(new Produk("Black Pen", 150, R.drawable.img, "Groceries"));
        items.add(new Produk("Fried Rice", 500, R.drawable.img, "Chilled"));
        items.add(new Produk("Mineral Water", 120, R.drawable.img, "Beverages"));
        return items;
    }
}
