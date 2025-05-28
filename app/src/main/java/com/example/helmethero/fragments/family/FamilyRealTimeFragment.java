package com.example.helmethero.fragments.family;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.helmethero.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ... your imports as above ...
public class FamilyRealTimeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private ProgressBar progressBarLoading;
    private FloatingActionButton btnRecenterMap;
    private final Map<String, Marker> riderMarkers = new HashMap<>();
    private final Map<String, Boolean> riderTripActive = new HashMap<>();
    private final Map<String, LatLng> lastMarkerPosition = new HashMap<>();
    private final List<String> linkedRiderIds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_real_time, container, false);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        btnRecenterMap = view.findViewById(R.id.btnRecenterMap);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.familyMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnRecenterMap.setOnClickListener(v -> recenterAllRiders());

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Center to Malaysia by default
        LatLng malaysiaCenter = new LatLng(3.139, 101.6869);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(malaysiaCenter, 6.5f));

        map.setOnMarkerClickListener(marker -> {
            String riderId = (String) marker.getTag();
            LatLng pos = marker.getPosition();
            if (riderId != null) {
                RiderDetailBottomSheet bottomSheet = RiderDetailBottomSheet.newInstance(riderId, pos.latitude, pos.longitude);
                bottomSheet.show(getChildFragmentManager(), "RiderDetailBottomSheet");
            }
            return true;
        });

        loadRidersByEmergencyContact();
    }

    private void loadRidersByEmergencyContact() {
        progressBarLoading.setVisibility(View.VISIBLE);
        String familyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridersRef = FirebaseDatabase.getInstance().getReference("Riders");

        ridersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBarLoading.setVisibility(View.GONE);
                linkedRiderIds.clear();
                for (DataSnapshot riderSnap : snapshot.getChildren()) {
                    DataSnapshot contactsSnap = riderSnap.child("emergencyContacts");
                    if (contactsSnap.child(familyId).exists()) {
                        linkedRiderIds.add(riderSnap.getKey());
                    }
                }
                startListeningAllRiders();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBarLoading.setVisibility(View.GONE);
            }
        });
    }

    private void startListeningAllRiders() {
        if (map != null) map.clear();
        riderMarkers.clear();
        riderTripActive.clear();
        lastMarkerPosition.clear();

        for (String riderId : linkedRiderIds) {
            listenToRiderLiveTracking(riderId);
        }
    }

    private void listenToRiderLiveTracking(String riderId) {
        DatabaseReference liveRef = FirebaseDatabase.getInstance()
                .getReference("Riders").child(riderId).child("liveTracking");
        liveRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String location = snapshot.child("location").getValue(String.class);
                Boolean tripActive = snapshot.child("tripActive").getValue(Boolean.class);
                if (tripActive == null) tripActive = false;

                double lat = 0, lng = 0;
                if (location != null && location.contains(",")) {
                    String[] locParts = location.split(",");
                    try {
                        lat = Double.parseDouble(locParts[0]);
                        lng = Double.parseDouble(locParts[1]);
                    } catch (Exception ignored) {}
                }
                LatLng pos = new LatLng(lat, lng);
                riderTripActive.put(riderId, tripActive);

                setCustomProfileMarker(riderId, pos, tripActive);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setCustomProfileMarker(String riderId, LatLng pos, boolean tripActive) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(riderId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String profileUrl = snapshot.child("profileImageUrl").getValue(String.class);

                View markerView = LayoutInflater.from(getContext()).inflate(
                        tripActive ? R.layout.marker_profile_active : R.layout.marker_profile_inactive, null);

                ImageView imgProfile = markerView.findViewById(R.id.imgProfileMarker);

                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(FamilyRealTimeFragment.this)
                            .asBitmap()
                            .load(profileUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(new CustomTarget<Bitmap>(100, 100) {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    if (!tripActive) {
                                        // Make profile grayscale for inactive
                                        android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
                                        matrix.setSaturation(0);
                                        imgProfile.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                                    }
                                    imgProfile.setImageBitmap(resource);
                                    Bitmap markerBitmap = createBitmapFromView(markerView);
                                    animateOrUpdateMarker(riderId, pos, markerBitmap);
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) { }
                            });
                } else {
                    imgProfile.setImageResource(R.drawable.ic_profile);
                    if (!tripActive) {
                        android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
                        matrix.setSaturation(0);
                        imgProfile.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                    }
                    Bitmap markerBitmap = createBitmapFromView(markerView);
                    animateOrUpdateMarker(riderId, pos, markerBitmap);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private Bitmap createBitmapFromView(View view) {
        int specWidth = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int specHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(specWidth, specHeight);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void animateOrUpdateMarker(String riderId, LatLng newPos, Bitmap bitmap) {
        Marker marker = riderMarkers.get(riderId);
        LatLng lastPos = lastMarkerPosition.get(riderId);

        if (marker == null && map != null) {
            marker = map.addMarker(new MarkerOptions()
                    .position(newPos)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .anchor(0.5f, 1f));
            marker.setTag(riderId);
            riderMarkers.put(riderId, marker);
            lastMarkerPosition.put(riderId, newPos);
        } else if (marker != null && lastPos != null) {
            // Animate marker
            android.animation.ValueAnimator latLngAnimator = android.animation.ValueAnimator.ofFloat(0, 1);
            LatLng start = lastPos, end = newPos;
            latLngAnimator.setDuration(600); // 0.6s animation
            Marker finalMarker = marker;
            latLngAnimator.addUpdateListener(animation -> {
                float v = (float) animation.getAnimatedValue();
                double lat = start.latitude + (end.latitude - start.latitude) * v;
                double lng = start.longitude + (end.longitude - start.longitude) * v;
                finalMarker.setPosition(new LatLng(lat, lng));
            });
            latLngAnimator.start();
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
            lastMarkerPosition.put(riderId, newPos);
        } else if (marker != null) {
            marker.setPosition(newPos);
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
            lastMarkerPosition.put(riderId, newPos);
        }
    }

    private void recenterAllRiders() {
        if (!riderMarkers.isEmpty() && map != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : riderMarkers.values()) {
                builder.include(marker.getPosition());
            }
            int padding = 150;
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
        }
    }
}