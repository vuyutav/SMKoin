package com.example.smkoin;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private int currentDestination = R.id.nav_home;
    private boolean protectedUiReady;
    private boolean resumeSessionCheck;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthGate.requireSession(this, () -> showMainUi(savedInstanceState));
    }

    private void showMainUi(Bundle savedInstanceState) {
        if (isFinishing()) return;
        protectedUiReady = true;
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (savedInstanceState != null) currentDestination = savedInstanceState.getInt("destination", R.id.nav_home);
        showDestination(currentDestination, false);
        bottomNavigationView.setSelectedItemId(currentDestination);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                showDestination(item.getItemId(), false);
                return true;
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (!protectedUiReady || resumeSessionCheck) return;
        resumeSessionCheck = true;
        SupabaseRepository.get(this).validateSession((valid, error) -> {
            if (isFinishing()) return;
            resumeSessionCheck = false;
            if (error != null || !Boolean.TRUE.equals(valid)) AuthGate.redirect(this, error);
        });
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("destination", currentDestination);
        super.onSaveInstanceState(outState);
    }

    private void showDestination(int id, boolean addToBackStack) {
        Fragment fragment;
        if (id == R.id.nav_scan) fragment = new ScanFragment();
        else if (id == R.id.nav_shop) fragment = new PointShopFragment();
        else if (id == R.id.nav_history) fragment = new HistoryFragment();
        else if (id == R.id.nav_profile) fragment = new ProfileFragment();
        else { id = R.id.nav_home; fragment = new HomeFragment(); }
        currentDestination = id;
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction().replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack("tab_" + id);
        transaction.commit();
    }

    void openTab(int id) {
        bottomNavigationView.setSelectedItemId(id);
    }
}
