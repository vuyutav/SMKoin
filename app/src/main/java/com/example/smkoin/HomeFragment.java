package com.example.smkoin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvProduk;
    private ProdukAdapter produkAdapter;
    private List<Produk> listProduk;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvProduk = view.findViewById(R.id.rvProduk);

        // Menggunakan GridLayoutManager agar produk tampil 2 kolom (seperti e-commerce)
        rvProduk.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Menyiapkan Data Dummy
        listProduk = new ArrayList<>();
        listProduk.add(new Produk("Roti Bakar Coklat", 250, R.drawable.img));
        listProduk.add(new Produk("Es Teh Manis", 100, R.drawable.img));
        listProduk.add(new Produk("Buku Tulis Sinar Dunia", 400, R.drawable.img));
        listProduk.add(new Produk("Pulpen Faster Hitam", 150, R.drawable.img));
        listProduk.add(new Produk("Nasi Goreng Kantin", 500, R.drawable.img));
        listProduk.add(new Produk("Air Mineral 600ml", 120, R.drawable.img));

        // Menghubungkan Adapter
        produkAdapter = new ProdukAdapter(listProduk);
        rvProduk.setAdapter(produkAdapter);

        return view;
    }
}