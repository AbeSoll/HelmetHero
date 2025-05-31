package com.example.helmethero.fragments.rider;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.activities.RiderHomeActivity;
import com.example.helmethero.services.LocationForegroundService;
import com.example.helmethero.utils.HelmetConnectionManager;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Locale;

public class RiderTripFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView durationText, distanceText, speedText;
    private TextView helmetStatusText;
    private View btnRecenter;

    private long startTimeMillis;
    private double totalDistanceKm = 0.0;
    private Location lastLocation;
    private final ArrayList<LatLng> routePoints = new ArrayList<>();

    private boolean isFollowingPointer = true;

    private final Handler durationHandler = new Handler(Looper.getMainLooper());
    private final Runnable durationRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurationUI();
            durationHandler.postDelayed(this, 1000);
        }
    };

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CODE_FOREGROUND_LOCATION = 2024;
    private String riderUid;

    private SharedPreferences prefs;

    public RiderTripFragment() {}

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip, container, false);

        prefs = requireActivity().getSharedPreferences("HelmetHeroPrefs", Context.MODE_PRIVATE);

        // Setup Google Map
        if (getChildFragmentManager().findFragmentById(R.id.map_container) == null) {
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
            mapFragment.getMapAsync(this);
        }

        // Defensive: always init fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Start foreground service for background location tracking (with permission check!)
        if (Build.VERSION.SDK_INT >= 34) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION}, REQUEST_CODE_FOREGROUND_LOCATION);
            } else {
                startForegroundLocationService();
            }
        } else {
            startForegroundLocationService();
        }

        // View bindings
        durationText = view.findViewById(R.id.textDuration);
        distanceText = view.findViewById(R.id.textDistance);
        speedText = view.findViewById(R.id.textCurrentSpeed);
        helmetStatusText = view.findViewById(R.id.textHelmetStatus);
        Button endTripButton = view.findViewById(R.id.btnEndTrip);
        Button sosButton = view.findViewById(R.id.btnSos);
        btnRecenter = view.findViewById(R.id.btnRecenter);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ProgressBar loadingSos = view.findViewById(R.id.loadingSos);

        btnRecenter.setVisibility(View.GONE); // Default: hide

        btnRecenter.setOnClickListener(v -> {
            if (lastLocation != null && map != null) {
                isFollowingPointer = true;
                LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
                btnRecenter.setVisibility(View.GONE);
            }
        });

        // Hide bottom nav when active
        view.post(() -> {
            if (getActivity() instanceof RiderHomeActivity) {
                ((RiderHomeActivity) getActivity()).setBottomNavVisibility(false);
            }
        });

        // Lock screen orientation and keep screen on
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Mark trip as active in Firebase
        riderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setTripActiveFlag(true);

        // === Restore Trip State (Bundle args > SharedPreferences > New Trip) ===
        boolean isResumeTrip = false;
        long resumeTripStartSystemTime = 0L;
        double resumeTripDistance = 0.0;
        String resumeRoutePointsJson = null;
        Bundle args = getArguments();
        if (args != null && args.getBoolean("resumeTrip", false)) {
            isResumeTrip = true;
            resumeTripStartSystemTime = args.getLong("tripStartSystemTime", 0L);
            resumeTripDistance = args.getDouble("tripDistance", 0.0);
            resumeRoutePointsJson = args.getString("routePointsJson", null);
        }

        if (isResumeTrip && resumeTripStartSystemTime > 0) {
            // Restore from Bundle args (from Splash > RiderHomeActivity)
            startTimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - resumeTripStartSystemTime);
            totalDistanceKm = resumeTripDistance;
            restoreRoutePointsFromJson(resumeRoutePointsJson);
        } else if (prefs.getBoolean("tripActive", false)) {
            // Restore from SharedPreferences (fallback if Bundle missing)
            startTimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - prefs.getLong("tripStartSystemTime", System.currentTimeMillis()));
            totalDistanceKm = Double.longBitsToDouble(prefs.getLong("tripDistance", 0));
            restoreRoutePointsFromPrefs();
        } else {
            startTimeMillis = SystemClock.elapsedRealtime();
            totalDistanceKm = 0.0;
            routePoints.clear();
        }

        durationHandler.post(durationRunnable);

        // End Trip Logic (STOP service when trip ends)
        endTripButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("End Trip?")
                    .setMessage("Are you sure you want to end this trip?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        long duration = SystemClock.elapsedRealtime() - startTimeMillis;
                        double avgSpeed = totalDistanceKm / (duration / 3600000.0);

                        setTripActiveFlag(false);
                        clearTripStatePrefs();

                        // Stop foreground service
                        requireActivity().stopService(new Intent(requireContext(), LocationForegroundService.class));

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

        // Block Back Navigation during active trip
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(getContext(), "Please end your trip before exiting.", Toast.LENGTH_SHORT).show();
            }
        });

        // SOS Button Logic (Animation + Vibration + Firebase)
        sosButton.setOnClickListener(v -> {
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                    v,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
            );
            pulse.setDuration(300);
            pulse.setInterpolator(new android.view.animation.CycleInterpolator(1));
            pulse.start();

            // Vibrate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }

            // Trigger SOS to Firebase
            loadingSos.setVisibility(View.VISIBLE);
            String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Riders").child(userUid).child("liveTracking");

            java.util.Map<String, Object> sosData = new java.util.HashMap<>();
            sosData.put("sosAlert", true);
            sosData.put("alert", "IMPACT");
            String timeNow = getCurrentTime();
            sosData.put("alertTime", timeNow);

            String locationStr;
            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double lng = lastLocation.getLongitude();
                locationStr = lat + "," + lng;
            } else {
                locationStr = "0,0";
                Toast.makeText(requireContext(), "Location unavailable. Using default coordinates.", Toast.LENGTH_SHORT).show();
            }
            sosData.put("location", locationStr);

            ref.updateChildren(sosData).addOnCompleteListener(task -> {
                loadingSos.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "SOS sent to Firebase!", Toast.LENGTH_SHORT).show();
            });

            // Push alert to all family contacts
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
                                        "IMPACT",
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

                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
            });
        });

        return view;
    }

    // Foreground Location Service launcher
    private void startForegroundLocationService() {
        Intent serviceIntent = new Intent(requireContext(), LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent);
        } else {
            requireActivity().startService(serviceIntent);
        }
    }

    // === tripActive flag setter ===
    private void setTripActiveFlag(boolean active) {
        if (riderUid == null) return;
        DatabaseReference liveRef = FirebaseDatabase.getInstance()
                .getReference("Riders").child(riderUid).child("liveTracking");
        liveRef.child("tripActive").setValue(active);

        prefs.edit().putBoolean("tripActive", active).apply();
        if (active) {
            prefs.edit()
                    .putLong("tripStartSystemTime", System.currentTimeMillis())
                    .putLong("tripDistance", Double.doubleToRawLongBits(totalDistanceKm))
                    .putString("routePointsJson", routePointsToJson())
                    .apply();
        }
    }

    // === clear all trip state from SharedPreferences ===
    private void clearTripStatePrefs() {
        prefs.edit()
                .remove("tripActive")
                .remove("tripStartSystemTime")
                .remove("tripDistance")
                .remove("routePointsJson")
                .apply();
    }

    // === restore route points from Bundle args json (if any) ===
    private void restoreRoutePointsFromJson(String json) {
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                routePoints.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONArray latlng = arr.getJSONArray(i);
                    routePoints.add(new LatLng(latlng.getDouble(0), latlng.getDouble(1)));
                }
            } catch (JSONException e) {
                routePoints.clear();
            }
        }
    }

    // === restore route points from prefs (if any) ===
    private void restoreRoutePointsFromPrefs() {
        String json = prefs.getString("routePointsJson", null);
        restoreRoutePointsFromJson(json);
    }

    // === serialize routePoints to JSON ===
    private String routePointsToJson() {
        JSONArray arr = new JSONArray();
        try {
            for (LatLng latLng : routePoints) {
                JSONArray point = new JSONArray();
                point.put(latLng.latitude);
                point.put(latLng.longitude);
                arr.put(point);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
        return arr.toString();
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

        // RE-INIT FUSED LOCATION CLIENT HERE - SOLVES NULL!
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        }

        // Set default position to Malaysia
        LatLng malaysiaCenter = new LatLng(3.1390, 101.6869); // Kuala Lumpur
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(malaysiaCenter, 10f));

        // Add recenter logic on user gesture
        map.setOnCameraMoveStartedListener(reason -> {
            // Only show recenter button if user gesture, not by code
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingPointer = false;
                if (btnRecenter != null) btnRecenter.setVisibility(View.VISIBLE);
            }
        });

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
        if (requestCode == REQUEST_CODE_FOREGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startForegroundLocationService();
            } else {
                Toast.makeText(getContext(), "Foreground location permission required!", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (map != null) onMapReady(map);
            } else {
                Toast.makeText(getContext(), "❗ Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        }

        LocationRequest request = LocationRequest.create();
        request.setInterval(1000);
        request.setFastestInterval(500);
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
                saveTripStateToPrefs();
                drawRoute();
                if (isFollowingPointer && map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
                }
            }
        }

        lastLocation = location;
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        routePoints.add(point);
        saveTripStateToPrefs();
        drawRoute();
        if (isFollowingPointer && map != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
        }

        float speedMps = location.hasSpeed() ? location.getSpeed() : 0f;
        float speedKph = speedMps * 3.6f;

        if (speedKph < 1.0f) speedKph = 0.0f;

        speedText.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKph));

        // PUSH REAL-TIME LOCATION TO FIREBASE FOR FAMILY MONITORING
        if (riderUid != null) {
            DatabaseReference liveLocRef = FirebaseDatabase.getInstance()
                    .getReference("Riders").child(riderUid).child("liveTracking");
            String locStr = location.getLatitude() + "," + location.getLongitude();
            liveLocRef.child("location").setValue(locStr);
            liveLocRef.child("lastUpdate").setValue(getCurrentTime());
            liveLocRef.child("speed").setValue(String.format(Locale.getDefault(), "%.1f km/h", speedKph));
        }
    }

    private void saveTripStateToPrefs() {
        prefs.edit()
                .putLong("tripDistance", Double.doubleToRawLongBits(totalDistanceKm))
                .putString("routePointsJson", routePointsToJson())
                .apply();
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
        setTripActiveFlag(false);

        // Stop foreground service if fragment is destroyed (just in case)
        requireActivity().stopService(new Intent(requireContext(), LocationForegroundService.class));

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

    @SuppressLint({"SetTextI18n", "NewApi"})
    @Override
    public void onResume() {
        super.onResume();

        if (helmetStatusText != null && getView() != null) {
            ImageView helmetIcon = getView().findViewById(R.id.imgHelmetStatus);
            boolean connected = HelmetConnectionManager.isConnected();

            helmetStatusText.setText(connected ? "Helmet Connected" : "Helmet Not Connected");

            if (helmetIcon != null) {
                helmetIcon.setImageResource(connected ? R.drawable.ic_success_green : R.drawable.ic_error_red);
                helmetIcon.setTooltipText(connected ? "Helmet is connected via Bluetooth" : "Helmet not connected. Please check Bluetooth");
            }
        }
    }
}