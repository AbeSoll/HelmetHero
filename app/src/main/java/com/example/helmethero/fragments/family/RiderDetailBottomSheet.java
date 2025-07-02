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

    private ValueEventListener liveListener;
    private DatabaseReference liveRef;

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
        TextView tvLastUpdate = view.findViewById(R.id.textLastUpdate);
        ImageView btnNavigate = view.findViewById(R.id.btnNavigate);

        // Fetch rider details (static, only once)
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Live updates for trip status, speed, location, last update
        liveRef = FirebaseDatabase.getInstance().getReference("Riders")
                .child(riderId).child("liveTracking");
        liveListener = new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean tripActive = snapshot.child("tripActive").getValue(Boolean.class);
                String statusLabel = (tripActive != null && tripActive) ? "ACTIVE RIDE" : "NOT RIDING";
                tvStatus.setText("Status: " + statusLabel);

                // SAFE conversion for speed
                Object speedObj = snapshot.child("speed").getValue();
                String speed = speedObj != null ? String.valueOf(speedObj) : "0.0";
                tvSpeed.setText("Speed: " + speed);

                // SAFE conversion for location
                Object locObj = snapshot.child("location").getValue();
                String liveLocation = locObj != null ? String.valueOf(locObj) : "";
                if (liveLocation.contains(",")) {
                    tvLoc.setText("Location: " + liveLocation);
                } else {
                    tvLoc.setText("Location: " + lat + ", " + lng);
                }

                // SAFE conversion for lastUpdate
                Object lastUpdateObj = snapshot.child("lastUpdate").getValue();
                String lastUpdate = lastUpdateObj != null ? String.valueOf(lastUpdateObj) : "";
                if (!lastUpdate.isEmpty()) {
                    String formattedTime = getFormattedRelativeOrExactTime(lastUpdate);
                    tvLastUpdate.setText("Last update: " + formattedTime);
                } else {
                    tvLastUpdate.setText("Last update: -");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        liveRef.addValueEventListener(liveListener);

        btnNavigate.setOnClickListener(v -> openGoogleMaps(lat, lng));
    }

    @Override
    public void onDestroyView() {
        // Clean up listener to prevent memory leaks
        if (liveRef != null && liveListener != null) {
            liveRef.removeEventListener(liveListener);
        }
        super.onDestroyView();
    }

    // Relative time if recent, else formatted date
    private String getFormattedRelativeOrExactTime(String timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date past = sdf.parse(timestamp);
            if (past == null) return "-";

            long now = System.currentTimeMillis();
            long pastMillis = past.getTime();
            long diff = now - pastMillis;

            if (diff < DateUtils.DAY_IN_MILLIS) {
                return DateUtils.getRelativeTimeSpanString(
                        pastMillis, now, DateUtils.MINUTE_IN_MILLIS
                ).toString();
            } else {
                SimpleDateFormat exactFormat = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());
                return exactFormat.format(past);
            }
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