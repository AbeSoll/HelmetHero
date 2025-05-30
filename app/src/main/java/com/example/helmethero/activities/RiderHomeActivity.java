package com.example.helmethero.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.helmethero.R;
import com.example.helmethero.fragments.rider.RiderEmergencyContactFragment;
import com.example.helmethero.fragments.rider.RiderHelmetFragment;
import com.example.helmethero.fragments.rider.RiderHistoryFragment;
import com.example.helmethero.fragments.rider.RiderSettingsFragment;
import com.example.helmethero.fragments.rider.RiderStartTripFragment;
import com.example.helmethero.fragments.rider.RiderTripFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class RiderHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Fragment tags
    private static final String TAG_START_TRIP = "RiderStartTripFragment";
    private static final String TAG_HELMET = "RiderHelmetFragment";
    private static final String TAG_HISTORY = "RiderHistoryFragment";
    private static final String TAG_CONTACTS = "RiderEmergencyContactFragment";
    private static final String TAG_SETTINGS = "RiderSettingsFragment";
    private static final String TAG_TRIP = "RiderTripFragment";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_home);

        // Setup Toolbar as ActionBar
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText("Helmet Hero");

        // Initialize Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String tag = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new RiderStartTripFragment();
                    tag = TAG_START_TRIP;
                } else if (itemId == R.id.nav_trip) {
                    selectedFragment = new RiderHelmetFragment();
                    tag = TAG_HELMET;
                } else if (itemId == R.id.nav_history) {
                    selectedFragment = new RiderHistoryFragment();
                    tag = TAG_HISTORY;
                } else if (itemId == R.id.nav_contacts) {
                    selectedFragment = new RiderEmergencyContactFragment();
                    tag = TAG_CONTACTS;
                } else if (itemId == R.id.nav_settings) {
                    selectedFragment = new RiderSettingsFragment();
                    tag = TAG_SETTINGS;
                } else {
                    return false;
                }

                if (selectedFragment != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setReorderingAllowed(true);
                    transaction.replace(R.id.fragment_container, selectedFragment, tag);
                    transaction.commit();
                    return true;
                }

                return false;
            }
        });

        // ==== AUTO-RESUME TRIP FEATURE ====
        boolean resumeTrip = getIntent().getBooleanExtra("resumeTrip", false);
        long tripStartSystemTime = getIntent().getLongExtra("tripStartSystemTime", 0L);
        double tripDistance = getIntent().getDoubleExtra("tripDistance", 0.0);
        String routePointsJson = getIntent().getStringExtra("routePointsJson");

        if (savedInstanceState == null) {
            if (resumeTrip) {
                // Pass semua state ke RiderTripFragment
                Bundle args = new Bundle();
                args.putBoolean("resumeTrip", true);
                args.putLong("tripStartSystemTime", tripStartSystemTime);
                args.putDouble("tripDistance", tripDistance);
                args.putString("routePointsJson", routePointsJson);

                RiderTripFragment riderTripFragment = new RiderTripFragment();
                riderTripFragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, riderTripFragment, TAG_TRIP)
                        .commit();

                // Set bottom nav highlight ke Home (atau disable highlight)
                setBottomNavSelected(R.id.nav_home); // optional, sebab trip fragment tak ada tab sendiri
            } else {
                // Default: buka home start trip
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, new RiderStartTripFragment(), TAG_START_TRIP)
                        .commit();
            }
        }
    }

    // Untuk sembunyikan/tunjuk bottom nav dari fragment
    public void setBottomNavVisibility(boolean show) {
        if (bottomNav != null) {
            bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // === NEW: Untuk force update selected nav bar icon dari fragment ===
    public void setBottomNavSelected(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }
}