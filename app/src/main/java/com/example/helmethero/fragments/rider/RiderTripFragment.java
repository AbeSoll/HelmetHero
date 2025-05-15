package com.example.helmethero.fragments.rider;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class RiderTripFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView durationText, distanceText, speedText;
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
        TextView helmetStatusText = view.findViewById(R.id.textHelmetStatus);
        Button endTripButton = view.findViewById(R.id.btnEndTrip);

        helmetStatusText.setText("🟢 Helmet Connected");

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


        endTripButton.setOnClickListener(v -> {
            long duration = SystemClock.elapsedRealtime() - startTimeMillis;
            double avgSpeed = totalDistanceKm / (duration / 3600000.0);

            RiderTripSummaryFragment summaryFragment =
                    RiderTripSummaryFragment.newInstance(duration, totalDistanceKm, avgSpeed, routePoints);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, summaryFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
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
            totalDistanceKm += distance / 1000.0;
            updateDistanceUI();
        }

        lastLocation = location;
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        routePoints.add(point);
        drawRoute();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));

        // ✅ Real-time speed update (safe fallback)
        if (location.hasSpeed()) {
            float speedMps = location.getSpeed(); // meters/second
            float speedKph = speedMps * 3.6f;
            speedText.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKph));
        } else {
            speedText.setText("0.0 km/h");
        }
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
        // Restore nav bar only when view is removed
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
}