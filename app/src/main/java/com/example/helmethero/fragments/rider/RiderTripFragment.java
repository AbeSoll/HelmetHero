package com.example.helmethero.fragments.rider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.activities.RiderHomeActivity;
import com.example.helmethero.utils.HelmetConnectionManager;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class RiderTripFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView durationText, distanceText, speedText;
    private TextView helmetStatusText;

    private long startTimeMillis;
    private double totalDistanceKm = 0.0;
    private Location lastLocation;
    private final ArrayList<LatLng> routePoints = new ArrayList<>();

    private final Handler durationHandler = new Handler(Looper.getMainLooper());
    private final Runnable durationRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurationUI();
            durationHandler.postDelayed(this, 1000);
        }
    };

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // === tripActive flag logic ===
    private String riderUid;

    public RiderTripFragment() {}

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip, container, false);

        if (getChildFragmentManager().findFragmentById(R.id.map_container) == null) {
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
            mapFragment.getMapAsync(this);
        }

        durationText = view.findViewById(R.id.textDuration);
        distanceText = view.findViewById(R.id.textDistance);
        speedText = view.findViewById(R.id.textCurrentSpeed);

        Button endTripButton = view.findViewById(R.id.btnEndTrip);
        helmetStatusText = view.findViewById(R.id.textHelmetStatus);

        view.post(() -> {
            if (getActivity() instanceof RiderHomeActivity) {
                ((RiderHomeActivity) getActivity()).setBottomNavVisibility(false);
            }
        });

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        startTimeMillis = SystemClock.elapsedRealtime();
        durationHandler.post(durationRunnable);

        // === Set tripActive flag: true at trip start ===
        riderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setTripActiveFlag(true);

        endTripButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("End Trip?")
                    .setMessage("Are you sure you want to end this trip?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        long duration = SystemClock.elapsedRealtime() - startTimeMillis;
                        double avgSpeed = totalDistanceKm / (duration / 3600000.0);

                        // === Set tripActive flag: false at trip end ===
                        setTripActiveFlag(false);

                        RiderTripSummaryFragment summaryFragment =
                                RiderTripSummaryFragment.newInstance(duration, totalDistanceKm, avgSpeed, routePoints);

                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, summaryFragment)
                                .addToBackStack(null)
                                .commit();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        Button sosButton = view.findViewById(R.id.btnSos);
        sosButton.setOnClickListener(v -> {
            String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref =
                    FirebaseDatabase.getInstance()
                            .getReference("Riders").child(userUid).child("liveTracking");

            java.util.Map<String, Object> sosData = new java.util.HashMap<>();
            sosData.put("sosAlert", true);
            sosData.put("alert", "IMPACT"); // or "TILT"
            sosData.put("alertTime", getCurrentTime());

            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double lng = lastLocation.getLongitude();
                sosData.put("location", lat + "," + lng);
            } else {
                sosData.put("location", "0,0");
                Toast.makeText(requireContext(), "Location unavailable. Using default coordinates.", Toast.LENGTH_SHORT).show();
            }

            ref.updateChildren(sosData);
            Toast.makeText(requireContext(), "SOS sent to Firebase!", Toast.LENGTH_SHORT).show();
        });

        // ... inside onCreateView, after ref.updateChildren(sosData);

        sosButton.setOnClickListener(v -> {
            String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref =
                    FirebaseDatabase.getInstance()
                            .getReference("Riders").child(userUid).child("liveTracking");

            java.util.Map<String, Object> sosData = new java.util.HashMap<>();
            sosData.put("sosAlert", true);
            sosData.put("alert", "IMPACT"); // or "TILT"
            String timeNow = getCurrentTime();
            sosData.put("alertTime", timeNow);

            String locationStr;
            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double lng = lastLocation.getLongitude();
                locationStr = lat + "," + lng;
                sosData.put("location", locationStr);
            } else {
                locationStr = "0,0";
                sosData.put("location", locationStr);
                Toast.makeText(requireContext(), "Location unavailable. Using default coordinates.", Toast.LENGTH_SHORT).show();
            }

            ref.updateChildren(sosData);
            Toast.makeText(requireContext(), "SOS sent to Firebase!", Toast.LENGTH_SHORT).show();

            // --- ALSO add alert to all family members ---
            DatabaseReference riderRef = FirebaseDatabase.getInstance()
                    .getReference("Riders").child(userUid);

            riderRef.child("emergencyContacts").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    for (com.google.firebase.database.DataSnapshot contactSnap : snapshot.getChildren()) {
                        String familyUid = contactSnap.getKey();
                        if (familyUid == null) continue;

                        String alertId = FirebaseDatabase.getInstance().getReference("Alerts").child(familyUid).push().getKey();
                        if (alertId == null) continue;

                        // Optionally get rider info for display
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userUid);
                        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot userSnap) {
                                String riderName = userSnap.child("name").getValue(String.class);
                                String profileImageUrl = userSnap.child("profileImageUrl").getValue(String.class);

                                com.example.helmethero.models.Alert alert = new com.example.helmethero.models.Alert(
                                        alertId,
                                        userUid,
                                        riderName,
                                        profileImageUrl,
                                        "IMPACT", // or "SOS"
                                        timeNow,
                                        locationStr,
                                        "NEW",
                                        false
                                );

                                FirebaseDatabase.getInstance()
                                        .getReference("Alerts")
                                        .child(familyUid)
                                        .child(alertId)
                                        .setValue(alert);
                            }
                            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                        });
                    }
                }
                @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
        });

        return view;
    }

    // === tripActive flag setter ===
    private void setTripActiveFlag(boolean active) {
        if (riderUid == null) return;
        DatabaseReference liveRef = FirebaseDatabase.getInstance()
                .getReference("Riders").child(riderUid).child("liveTracking");
        liveRef.child("tripActive").setValue(active);
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        if (hasLocationPermission()) {
            map.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (map != null) onMapReady(map);
            } else {
                Toast.makeText(getContext(), "❗ Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(1000);             // Update every 1 second
        request.setFastestInterval(500);       // Accept updates as fast as 0.5 second
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    updateTrip(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateTrip(Location location) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            float accuracy = location.getAccuracy();

            if (distance >= 5 && accuracy <= 10) {
                totalDistanceKm += distance / 1000.0;
                updateDistanceUI();
                lastLocation = location;
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                routePoints.add(point);
                drawRoute();
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
            }
        }

        lastLocation = location;
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        routePoints.add(point);
        drawRoute();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));

        float speedMps = location.hasSpeed() ? location.getSpeed() : 0f;
        float speedKph = speedMps * 3.6f;

        if (speedKph < 1.0f) speedKph = 0.0f;

        speedText.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKph));
    }

    private void drawRoute() {
        if (routePoints.size() < 2) return;

        map.clear();
        map.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .width(10f)
                .color(requireContext().getColor(R.color.helmet_blue))
                .geodesic(true));
    }

    private void updateDistanceUI() {
        distanceText.setText(String.format(Locale.getDefault(), "%.2f km", totalDistanceKm));
    }

    private void updateDurationUI() {
        long elapsedMillis = SystemClock.elapsedRealtime() - startTimeMillis;
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) ((elapsedMillis % 3600000) / 60000);
        int seconds = (int) ((elapsedMillis % 60000) / 1000);
        durationText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // === Robustly set tripActive: false if user leaves this fragment (unexpectedly) ===
        setTripActiveFlag(false);

        if (getActivity() instanceof RiderHomeActivity) {
            ((RiderHomeActivity) getActivity()).setBottomNavVisibility(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        durationHandler.removeCallbacks(durationRunnable);
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
        if (helmetStatusText != null) {
            if (HelmetConnectionManager.isConnected()) {
                helmetStatusText.setText("✅ Helmet Connected");
            } else {
                helmetStatusText.setText("❌ Helmet Not Connected");
            }
        }
    }
}