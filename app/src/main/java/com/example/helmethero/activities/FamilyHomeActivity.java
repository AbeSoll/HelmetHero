package com.example.helmethero.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.example.helmethero.fragments.family.FamilyAlertFragment;
import com.example.helmethero.fragments.family.FamilyDashboardFragment;
import com.example.helmethero.fragments.family.FamilyLinkedRidersFragment;
import com.example.helmethero.fragments.family.FamilyRealTimeFragment;
import com.example.helmethero.fragments.family.FamilySettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

public class FamilyHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageView notificationIcon, profileIcon;
    private TextView notificationBadge;

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

        // Permission for notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // FCM token update
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
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText("Helmet Hero");

        // Notification bell and badge
        notificationIcon = toolbar.findViewById(R.id.notification_icon);
        notificationBadge = toolbar.findViewById(R.id.notification_badge);
        profileIcon = toolbar.findViewById(R.id.profile_icon);

        // Load profile image (from Firebase if you want, or use default)
        loadProfileImage();

        // Notification badge: update based on unread alert count from Firebase
        updateNotificationBadge();

        // Notification icon click -> go to Alert fragment
        notificationIcon.setOnClickListener(v -> switchToAlertFragment());

        // Profile icon click -> go to profile/settings
        profileIcon.setOnClickListener(v -> switchToSettingsFragment());

        // Bottom nav setup
        bottomNav = findViewById(R.id.bottomNavigation);

        // ---- Native animation on tab select ----
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Scale animation section
                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    MenuItem menuItem = bottomNav.getMenu().getItem(i);
                    final View view = bottomNav.findViewById(menuItem.getItemId());
                    if (view != null) {
                        if (menuItem == item) {
                            // Scale up + bounce + haptic
                            view.animate()
                                    .scaleX(1.12f)
                                    .scaleY(1.12f)
                                    .setDuration(180)
                                    .setInterpolator(new OvershootInterpolator())
                                    .start();
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        } else {
                            view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(150)
                                    .start();
                        }
                    }
                }

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

    // Helper: go to Alerts fragment when bell is tapped
    private void switchToAlertFragment() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.family_fragment_container, new FamilyAlertFragment(), TAG_ALERTS)
                .commit();
        // TIADA LAGI resetNotificationBadge() DI SINI!
    }

    // Helper: go to settings/profile when profile icon is tapped
    private void switchToSettingsFragment() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.family_fragment_container, new FamilySettingsFragment(), TAG_SETTINGS)
                .commit();
    }

    // Update notification badge count (from Firebase Alerts)
    private void updateNotificationBadge() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("Alerts").child(familyUid);

        alertsDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot alertSnap : snapshot.getChildren()) {
                    String status = alertSnap.child("status").getValue(String.class);
                    if (status != null && status.equalsIgnoreCase("NEW")) count++;
                }
                if (count > 0) {
                    notificationBadge.setVisibility(View.VISIBLE);
                    notificationBadge.setText(String.valueOf(count));
                } else {
                    notificationBadge.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // For badge: If ever want to hide/show manually (not auto-update)
    public void setNotificationBadgeVisibility(boolean show) {
        if (notificationBadge != null) {
            notificationBadge.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // Load profile image from Firebase
    private void loadProfileImage() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(FamilyHomeActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(profileIcon);
                } else {
                    profileIcon.setImageResource(R.drawable.ic_profile);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                profileIcon.setImageResource(R.drawable.ic_profile);
            }
        });
    }

    // For hiding/showing bottom nav from fragment
    public void setBottomNavVisibility(boolean show) {
        if (bottomNav != null) {
            bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}