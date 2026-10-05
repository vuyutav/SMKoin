package com.example.smkoin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        ListView lvRiwayat = view.findViewById(R.id.lvRiwayat);

        // Menggunakan layout kustom R.layout.item_riwayat dan ID TextView R.id.tvTextRiwayat
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_riwayat,
                R.id.tvTextRiwayat,
                DataRiwayat.listRiwayat
        );

        lvRiwayat.setAdapter(adapter);

        return view;
    }
}