package com.example.smkoin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ProdukAdapter extends RecyclerView.Adapter<ProdukAdapter.ProdukViewHolder> {

    private final List<Produk> listProduk = new ArrayList<>();

    public ProdukAdapter(List<Produk> listProduk) {
        this.listProduk.addAll(listProduk);
    }

    public void setProducts(List<Produk> products) {
        listProduk.clear();
        listProduk.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProdukViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produk, parent, false);
        return new ProdukViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdukViewHolder holder, int position) {
        Produk produk = listProduk.get(position);

        holder.tvNama.setText(produk.getNama());
        holder.tvHarga.setText(produk.getHargaKoin() + " pts");
        holder.ivGambar.setImageResource(produk.getGambarResId());
        holder.ivGambar.setContentDescription(produk.getNama() + " preview image");

        holder.itemView.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                // Membuat Intent untuk pindah ke DetailActivity
                android.content.Intent intent = new android.content.Intent(v.getContext(), DetailActivity.class);

                // Membawa data produk ke halaman detail
                intent.putExtra("EXTRA_NAMA", produk.getNama());
                intent.putExtra("EXTRA_HARGA", produk.getHargaKoin());
                intent.putExtra("EXTRA_GAMBAR", produk.getGambarResId());
                intent.putExtra("EXTRA_CATEGORY", produk.getCategory());

                // Menjalankan perpindahan halaman
                v.getContext().startActivity(intent);
            }
        });
        // ------------------------------
    }

    @Override
    public int getItemCount() {
        return listProduk.size();
    }

    public static class ProdukViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvHarga;
        ImageView ivGambar;

        public ProdukViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tvNamaProduk);
            tvHarga = itemView.findViewById(R.id.tvHargaKoin);
            ivGambar = itemView.findViewById(R.id.ivGambarProduk);
        }
    }
}
