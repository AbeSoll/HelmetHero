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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.helmethero.R;
import com.example.helmethero.fragments.rider.RiderEmergencyContactFragment;
import com.example.helmethero.fragments.rider.RiderHelmetFragment;
import com.example.helmethero.fragments.rider.RiderHistoryFragment;
import com.example.helmethero.fragments.rider.RiderSettingsFragment;
import com.example.helmethero.fragments.rider.RiderStartTripFragment;
import com.example.helmethero.fragments.rider.RiderTripFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

public class RiderHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageView profileImage;

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

        // ====== FCM TOKEN UPDATE (Always save on entering Home) =======
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken").setValue(token);
                }
            });
        }

        // Setup Toolbar as ActionBar
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText("Helmet Hero");

        profileImage = toolbar.findViewById(R.id.profile_image);

        // Load Profile Image from Firebase
        loadProfileImage();

        // Make profile image clickable: open settings/profile
        profileImage.setOnClickListener(v -> {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setReorderingAllowed(true);
            transaction.replace(R.id.fragment_container, new RiderSettingsFragment(), TAG_SETTINGS);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Initialize Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation);

        // Animation on tab select (native scale & bounce)
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Scale animation for each item
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
                // Pass all state to RiderTripFragment
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

            } else {
                // Default: open start trip home
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, new RiderStartTripFragment(), TAG_START_TRIP)
                        .commit();
            }
        }
    }

    // Load profile image from Firebase (SKIP CACHE)
    private void loadProfileImage() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String url = snapshot.getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        Glide.with(RiderHomeActivity.this)
                                .load(url)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .circleCrop()
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }
            });
        }
    }

    // PUBLIC: Refresh profile image (boleh dipanggil dari fragment selepas upload gambar)
    public void refreshProfileImage() {
        loadProfileImage();
    }

    // For showing/hiding bottom nav from fragment
    public void setBottomNavVisibility(boolean show) {
        if (bottomNav != null) {
            bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}