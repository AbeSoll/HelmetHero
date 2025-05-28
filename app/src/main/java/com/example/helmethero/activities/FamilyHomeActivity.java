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

// PERMISSION IMPORT
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.example.helmethero.R;
import com.example.helmethero.fragments.family.FamilyAlertFragment;
import com.example.helmethero.fragments.family.FamilyDashboardFragment;
import com.example.helmethero.fragments.family.FamilyLinkedRidersFragment;
import com.example.helmethero.fragments.family.FamilyRealTimeFragment;
import com.example.helmethero.fragments.family.FamilySettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

// Tambah import untuk FCM
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class FamilyHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Fragment tags (optional)
    private static final String TAG_HOME = "FamilyDashboardFragment";
    private static final String TAG_TRIP = "FamilyRealTimeFragment";
    private static final String TAG_HISTORY = "FamilyLinkedRidersFragment";
    private static final String TAG_ALERTS = "FamilyAlertFragment";
    private static final String TAG_SETTINGS = "FamilySettingsFragment";

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_home);

        // === PERMISSION (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // Paksa update FCM token masa family buka home
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uid != null) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken");
                        ref.setValue(token);
                    }
                });

        // Setup Toolbar
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Toolbar toolbar = findViewById(R.id.top_toolbar);
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText("Helmet Hero");

        bottomNav = findViewById(R.id.bottomNavigation); // ✅ Match XML ID

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String tag = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new FamilyDashboardFragment();
                    tag = TAG_HOME;
                } else if (itemId == R.id.nav_trip) {
                    selectedFragment = new FamilyRealTimeFragment();
                    tag = TAG_TRIP;
                } else if (itemId == R.id.nav_history) {
                    selectedFragment = new FamilyLinkedRidersFragment();
                    tag = TAG_HISTORY;
                } else if (itemId == R.id.nav_alerts) {
                    selectedFragment = new FamilyAlertFragment();
                    tag = TAG_ALERTS;
                } else if (itemId == R.id.nav_settings) {
                    selectedFragment = new FamilySettingsFragment();
                    tag = TAG_SETTINGS;
                } else {
                    return false;
                }

                if (selectedFragment != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setReorderingAllowed(true);
                    transaction.replace(R.id.family_fragment_container, selectedFragment, tag);
                    transaction.commit();
                    return true;
                }

                return false;
            }
        });

        // Default Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.family_fragment_container, new FamilyDashboardFragment(), TAG_HOME)
                    .commit();
        }
    }

    public void setBottomNavVisibility(boolean show) {
        if (bottomNav != null) {
            bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
