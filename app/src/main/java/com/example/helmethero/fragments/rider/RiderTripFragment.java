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
import android.os.CountDownTimer;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.activities.RiderHomeActivity;
import com.example.helmethero.models.Alert;
import com.example.helmethero.services.LocationForegroundService;
import com.example.helmethero.utils.HelmetConnectionManager;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RiderTripFragment extends Fragment implements OnMapReadyCallback {

    // Map and Location variables
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private final ArrayList<LatLng> routePoints = new ArrayList<>();
    private boolean isFollowingPointer = true;

    // UI Elements
    private TextView durationText, distanceText, speedText;
    private TextView helmetStatusText;
    private View btnRecenter;
    private ProgressBar loadingSos;

    // Trip State variables
    private long startTimeMillis;
    private double totalDistanceKm = 0.0;
    private final Handler durationHandler = new Handler(Looper.getMainLooper());
    private String riderUid;
    private SharedPreferences prefs;

    // --- NEW: SOS Countdown Timer ---
    private CountDownTimer sosCountdownTimer;

    // Permission Launchers
    private ActivityResultLauncher<String[]> foregroundServicePermissionLauncher;
    private ActivityResultLauncher<String[]> mapLocationPermissionLauncher;

    private final Runnable durationRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurationUI();
            durationHandler.postDelayed(this, 1000);
        }
    };

    public RiderTripFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = requireActivity().getSharedPreferences("HelmetHeroPrefs", Context.MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        initializePermissionLaunchers();
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip, container, false);

        // Setup Google Map
        if (getChildFragmentManager().findFragmentById(R.id.map_container) == null) {
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
            mapFragment.getMapAsync(this);
        }

        checkAndRequestForegroundServicePermission();

        // View bindings
        durationText = view.findViewById(R.id.textDuration);
        distanceText = view.findViewById(R.id.textDistance);
        speedText = view.findViewById(R.id.textCurrentSpeed);
        helmetStatusText = view.findViewById(R.id.textHelmetStatus);
        Button endTripButton = view.findViewById(R.id.btnEndTrip);
        Button sosButton = view.findViewById(R.id.btnSos);
        btnRecenter = view.findViewById(R.id.btnRecenter);
        loadingSos = view.findViewById(R.id.loadingSos);

        btnRecenter.setVisibility(View.GONE);

        btnRecenter.setOnClickListener(v -> {
            if (lastLocation != null && map != null) {
                isFollowingPointer = true;
                LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
                btnRecenter.setVisibility(View.GONE);
            }
        });

        view.post(() -> {
            if (getActivity() instanceof RiderHomeActivity) {
                ((RiderHomeActivity) getActivity()).setBottomNavVisibility(false);
            }
        });

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        riderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setTripActiveFlag(true);

        // Trip Resumption Logic
        handleTripResumption();

        durationHandler.post(durationRunnable);

        // End Trip Logic
        endTripButton.setOnClickListener(v -> showEndTripDialog());

        // Back Press Logic
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(getContext(), "Please end your trip before exiting.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- UPDATED: SOS Button Logic ---
        sosButton.setOnClickListener(v -> {
            // Pulse and vibrate for immediate feedback
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f));
            pulse.setDuration(300);
            pulse.setInterpolator(new android.view.animation.CycleInterpolator(1));
            pulse.start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
            // Show the new countdown dialog
            showSosCountdownDialog();
        });

        return view;
    }

    private void handleTripResumption() {
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
            startTimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - resumeTripStartSystemTime);
            totalDistanceKm = resumeTripDistance;
            restoreRoutePointsFromJson(resumeRoutePointsJson);
        } else if (prefs.getBoolean("tripActive", false)) {
            startTimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - prefs.getLong("tripStartSystemTime", System.currentTimeMillis()));
            totalDistanceKm = Double.longBitsToDouble(prefs.getLong("tripDistance", 0));
            restoreRoutePointsFromPrefs();
        } else {
            startTimeMillis = SystemClock.elapsedRealtime();
            totalDistanceKm = 0.0;
            routePoints.clear();
        }
    }

    private void showEndTripDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Trip?")
                .setMessage("Are you sure you want to end this trip?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    long duration = SystemClock.elapsedRealtime() - startTimeMillis;
                    double avgSpeed = (duration > 0) ? totalDistanceKm / (duration / 3600000.0) : 0.0;

                    setTripActiveFlag(false);
                    clearTripStatePrefs();

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
    }

    // --- NEW: Method to show the SOS countdown dialog ---
    private void showSosCountdownDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sos_countdown, null);
        AlertDialog sosDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        TextView textCountdown = dialogView.findViewById(R.id.textCountdown);
        ProgressBar progressCountdown = dialogView.findViewById(R.id.progressCountdown);
        Button btnCancelSos = dialogView.findViewById(R.id.btnCancelSos);

        sosCountdownTimer = new CountDownTimer(10000, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished / 1000) + 1;
                textCountdown.setText(String.valueOf(secondsLeft));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressCountdown.setProgress((int) secondsLeft, true);
                } else {
                    progressCountdown.setProgress((int) secondsLeft);
                }
            }

            @Override
            public void onFinish() {
                sosDialog.dismiss();
                sendSosAlert();
            }
        };

        btnCancelSos.setOnClickListener(v -> {
            sosCountdownTimer.cancel();
            sosDialog.dismiss();
            Toast.makeText(getContext(), "SOS Canceled", Toast.LENGTH_SHORT).show();
        });

        sosCountdownTimer.start();
        sosDialog.show();
    }

    // --- NEW: Method containing the original SOS logic ---
    private void sendSosAlert() {
        if (getContext() == null) return;

        loadingSos.setVisibility(View.VISIBLE);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Riders").child(userUid).child("liveTracking");

        Map<String, Object> sosData = new HashMap<>();
        sosData.put("sosAlert", true);
        sosData.put("alert", "IMPACT");
        String timeNow = getCurrentTime();
        sosData.put("alertTime", timeNow);

        String locationStr;
        if (lastLocation != null) {
            locationStr = lastLocation.getLatitude() + "," + lastLocation.getLongitude();
        } else {
            locationStr = "0,0";
            Toast.makeText(getContext(), "Location unavailable. Using default coordinates.", Toast.LENGTH_SHORT).show();
        }
        sosData.put("location", locationStr);

        ref.updateChildren(sosData).addOnCompleteListener(task -> {
            loadingSos.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "SOS sent to Firebase!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to send SOS. Check connection.", Toast.LENGTH_SHORT).show();
            }
        });

        // Notify emergency contacts
        notifyEmergencyContacts(userUid, timeNow, locationStr);
    }

    private void notifyEmergencyContacts(String userUid, String timeNow, String locationStr) {
        DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference("Riders").child(userUid);
        riderRef.child("emergencyContacts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot contactSnap : snapshot.getChildren()) {
                    String familyUid = contactSnap.getKey();
                    if (familyUid == null) continue;

                    String alertId = FirebaseDatabase.getInstance().getReference("Alerts").child(familyUid).push().getKey();
                    if (alertId == null) continue;

                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userUid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            String riderName = userSnap.child("name").getValue(String.class);
                            String profileImageUrl = userSnap.child("profileImageUrl").getValue(String.class);

                            Alert alert = new Alert(
                                    alertId, userUid, riderName, profileImageUrl,
                                    "IMPACT", timeNow, locationStr, "NEW", false
                            );

                            FirebaseDatabase.getInstance().getReference("Alerts")
                                    .child(familyUid).child(alertId).setValue(alert);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Log error or handle
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error or handle
            }
        });
    }


    private void initializePermissionLaunchers() {
        foregroundServicePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                        startForegroundLocationService();
                    } else {
                        Toast.makeText(getContext(), "Foreground location permission required!", Toast.LENGTH_LONG).show();
                    }
                });

        mapLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                        initializeMapWithLocation();
                    } else {
                        Toast.makeText(getContext(), "❗ Location permission denied", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndRequestForegroundServicePermission() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ for notifications
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        }


        ArrayList<String> permissionsNotGranted = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ActivityCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNotGranted.add(permission);
            }
        }

        if (!permissionsNotGranted.isEmpty()) {
            foregroundServicePermissionLauncher.launch(permissionsNotGranted.toArray(new String[0]));
        } else {
            startForegroundLocationService();
        }
    }


    private void startForegroundLocationService() {
        Intent serviceIntent = new Intent(requireContext(), LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent);
        } else {
            requireActivity().startService(serviceIntent);
        }
    }

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

    private void clearTripStatePrefs() {
        prefs.edit()
                .remove("tripActive")
                .remove("tripStartSystemTime")
                .remove("tripDistance")
                .remove("routePointsJson")
                .apply();
    }

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

    private void restoreRoutePointsFromPrefs() {
        String json = prefs.getString("routePointsJson", null);
        restoreRoutePointsFromJson(json);
    }

    private String routePointsToJson() {
        JSONArray arr = new JSONArray();
        for (LatLng latLng : routePoints) {
            try {
                JSONArray point = new JSONArray();
                point.put(latLng.latitude);
                point.put(latLng.longitude);
                arr.put(point);
            } catch (JSONException e) {
                // Should not happen
            }
        }
        return arr.toString();
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        LatLng malaysiaCenter = new LatLng(3.1390, 101.6869); // Kuala Lumpur
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(malaysiaCenter, 10f));

        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingPointer = false;
                if (btnRecenter != null) btnRecenter.setVisibility(View.VISIBLE);
            }
        });

        if (hasLocationPermission()) {
            initializeMapWithLocation();
        } else {
            mapLocationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeMapWithLocation() {
        if (map == null || !hasLocationPermission()) return;
        map.setMyLocationEnabled(true);
        startLocationUpdates();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    updateTrip(location);
                }
            }
        };

        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateTrip(Location location) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            if (distance > 2 && location.getAccuracy() < 20) {
                totalDistanceKm += distance / 1000.0;
                updateDistanceUI();
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                routePoints.add(point);
                drawRoute();
                if (isFollowingPointer && map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
                }
            }
        } else {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            routePoints.add(point);
            drawRoute();
            if (isFollowingPointer && map != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
            }
        }
        lastLocation = location;
        saveTripStateToPrefs();

        float speedKph = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
        speedText.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKph));

        if (riderUid != null) {
            DatabaseReference liveLocRef = FirebaseDatabase.getInstance()
                    .getReference("Riders").child(riderUid).child("liveTracking");
            liveLocRef.child("location").setValue(location.getLatitude() + "," + location.getLongitude());
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
        if (map == null || routePoints.size() < 2) return;
        map.clear();
        map.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .width(12f)
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
    public void onResume() {
        super.onResume();
        if (helmetStatusText != null && getView() != null) {
            ImageView helmetIcon = getView().findViewById(R.id.imgHelmetStatus);
            boolean connected = HelmetConnectionManager.isConnected();
            helmetStatusText.setText(connected ? "Helmet Connected" : "Helmet Not Connected");
            if (helmetIcon != null) {
                helmetIcon.setImageResource(connected ? R.drawable.ic_success_green : R.drawable.ic_error_red);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (prefs.getBoolean("tripActive", false)) {
            saveTripStateToPrefs();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // --- NEW: Cancel the timer to prevent memory leaks ---
        if (sosCountdownTimer != null) {
            sosCountdownTimer.cancel();
        }
        if (getActivity() instanceof RiderHomeActivity) {
            ((RiderHomeActivity) getActivity()).setBottomNavVisibility(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        durationHandler.removeCallbacks(durationRunnable);
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
