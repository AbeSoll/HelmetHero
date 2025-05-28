package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RiderDetailBottomSheet extends BottomSheetDialogFragment {

    private String riderId;
    private double lat, lng;

    public static RiderDetailBottomSheet newInstance(String riderId, double lat, double lng) {
        RiderDetailBottomSheet fragment = new RiderDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString("riderId", riderId);
        args.putDouble("lat", lat);
        args.putDouble("lng", lng);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_rider_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            riderId = getArguments().getString("riderId");
            lat = getArguments().getDouble("lat");
            lng = getArguments().getDouble("lng");
        }

        ImageView imgProfilePhoto = view.findViewById(R.id.imgProfilePhoto);
        TextView tvName = view.findViewById(R.id.textRiderName);
        TextView tvStatus = view.findViewById(R.id.textLiveStatus);
        TextView tvSpeed = view.findViewById(R.id.textLiveSpeed);
        TextView tvLoc = view.findViewById(R.id.textLiveLocation);
        TextView tvLastUpdate = view.findViewById(R.id.textLastUpdate); // NEW
        ImageView btnNavigate = view.findViewById(R.id.btnNavigate);

        // Fetch rider details from /Users/{riderId}
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(riderId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String profileUrl = snapshot.child("profileImageUrl").getValue(String.class);
                tvName.setText(name != null ? name : riderId);

                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(profileUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(imgProfilePhoto);
                } else {
                    imgProfilePhoto.setImageResource(R.drawable.ic_profile);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Fetch liveTracking for status, speed, location, and last update
        DatabaseReference liveRef = FirebaseDatabase.getInstance().getReference("Riders")
                .child(riderId).child("liveTracking");
        liveRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // --- Check for tripActive status ---
                Boolean tripActive = snapshot.child("tripActive").getValue(Boolean.class);
                String statusLabel = (tripActive != null && tripActive) ? "ACTIVE RIDE" : "NOT RIDING";
                tvStatus.setText("Status: " + statusLabel);

                String speed = snapshot.child("speed").getValue(String.class);
                tvSpeed.setText("Speed: " + (speed != null ? speed : "0.0 km/h"));
                tvLoc.setText("Location: " + lat + ", " + lng);

                // NEW: Last update (relative time)
                String lastUpdate = snapshot.child("lastUpdate").getValue(String.class);
                if (lastUpdate != null && !lastUpdate.isEmpty()) {
                    String prettyTime = getRelativeTime(lastUpdate);
                    tvLastUpdate.setText("Last update: " + prettyTime);
                } else {
                    tvLastUpdate.setText("Last update: -");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        btnNavigate.setOnClickListener(v -> openGoogleMaps(lat, lng));
    }

    // Converts "yyyy-MM-dd HH:mm:ss" to "just now", "3 min ago", "yesterday", etc.
    private String getRelativeTime(String lastUpdate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date past = sdf.parse(lastUpdate);
            if (past == null) return "-";
            long now = System.currentTimeMillis();
            long pastMillis = past.getTime();
            // Android built-in utility for relative time:
            return DateUtils.getRelativeTimeSpanString(
                    pastMillis, now, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
            ).toString();
        } catch (ParseException e) {
            return "-";
        }
    }

    private void openGoogleMaps(double lat, double lng) {
        String uri = "google.navigation:q=" + lat + "," + lng;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }
}