package com.example.helmethero.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.fragments.rider.RiderStartTripFragment;
import com.example.helmethero.fragments.rider.RiderTripFragment;
import com.example.helmethero.fragments.rider.RiderHistoryFragment;
import com.example.helmethero.fragments.rider.RiderEmergencyContactFragment;
import com.example.helmethero.fragments.rider.RiderSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RiderHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_home);

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.nav_home:
                    selectedFragment = new RiderStartTripFragment();
                    break;
                case R.id.nav_trip:
                    selectedFragment = new RiderTripFragment();
                    break;
                case R.id.nav_history:
                    selectedFragment = new RiderHistoryFragment();
                    break;
                case R.id.nav_contacts:
                    selectedFragment = new RiderEmergencyContactFragment();
                    break;
                case R.id.nav_settings:
                    selectedFragment = new RiderSettingsFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        // Set default fragment on first load
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RiderStartTripFragment())
                    .commit();
        }
    }
}
