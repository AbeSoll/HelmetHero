package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.helmethero.R;
import com.example.helmethero.activities.RiderHomeActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class RiderTripSummaryFragment extends Fragment implements OnMapReadyCallback {

    private TextView textDuration, textDistance, textAvgSpeed, textTripDate;
    private GoogleMap googleMap;

    private long tripDurationMillis;
    private double distanceKm;
    private double avgSpeed;
    private ArrayList<LatLng> routePoints;

    public static RiderTripSummaryFragment newInstance(long duration, double distance, double speed, ArrayList<LatLng> route) {
        RiderTripSummaryFragment fragment = new RiderTripSummaryFragment();
        Bundle args = new Bundle();
        args.putLong("duration", duration);
        args.putDouble("distance", distance);
        args.putDouble("speed", speed);
        args.putSerializable("routePoints", route);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip_summary, container, false);

        view.post(() -> {
            if (getActivity() instanceof RiderHomeActivity) {
                ((RiderHomeActivity) getActivity()).setBottomNavVisibility(false);
            }
        });

        textDuration = view.findViewById(R.id.textDuration);
        textDistance = view.findViewById(R.id.textDistance);
        textAvgSpeed = view.findViewById(R.id.textAvgSpeed);
        EditText editTripNote = view.findViewById(R.id.editTripNote);
        RadioGroup moodGroup = view.findViewById(R.id.radioMoodGroup);
        CheckBox tagTraffic = view.findViewById(R.id.tag_traffic);
        CheckBox tagWeather = view.findViewById(R.id.tag_weather);
        CheckBox tagHelmet = view.findViewById(R.id.tag_helmet);
        textTripDate = view.findViewById(R.id.textTripDate);
        Button btnSave = view.findViewById(R.id.btnSaveActivity);
        Button btnDiscard = view.findViewById(R.id.btnDiscardActivity);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.summaryMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Retrieve arguments
        if (getArguments() != null) {
            tripDurationMillis = getArguments().getLong("duration");
            distanceKm = getArguments().getDouble("distance");
            avgSpeed = getArguments().getDouble("speed");
            routePoints = (ArrayList<LatLng>) getArguments().getSerializable("routePoints");
        }

        int hours = (int) (tripDurationMillis / 3600000);
        int minutes = (int) (tripDurationMillis / 60000);
        int seconds = (int) ((tripDurationMillis % 60000) / 1000);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        textDuration.setText("Duration: " + String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
        textDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.2f km", distanceKm));
        textAvgSpeed.setText("Average Speed: " + String.format(Locale.getDefault(), "%.1f km/h", avgSpeed));
        textTripDate.setText("Date: " + timestamp);

        btnSave.setOnClickListener(v -> saveTripData());
        btnDiscard.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Discard Trip")
                    .setMessage("Are you sure you want to discard this trip?")
                    .setPositiveButton("Yes", (dialog, which) -> discardTrip())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        drawRoutePreview();
    }

    private void drawRoutePreview() {
        if (googleMap == null || routePoints == null || routePoints.size() < 2) return;

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .color(requireContext().getColor(R.color.helmet_blue))
                .width(10f);

        googleMap.addPolyline(polylineOptions);
        LatLng last = routePoints.get(routePoints.size() - 1);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(last, 16f));
    }

    private void saveTripData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "❗ No user logged in!", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Trips").child(uid);

        // Collect trip notes
        EditText editTripNote = requireView().findViewById(R.id.editTripNote);
        RadioGroup moodGroup = requireView().findViewById(R.id.radioMoodGroup);
        CheckBox tagTraffic = requireView().findViewById(R.id.tag_traffic);
        CheckBox tagWeather = requireView().findViewById(R.id.tag_weather);
        CheckBox tagHelmet = requireView().findViewById(R.id.tag_helmet);

        String noteText = editTripNote.getText().toString().trim();

        // Get selected emoji mood
        String selectedMood = "";
        int selectedId = moodGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selected = requireView().findViewById(selectedId);
            selectedMood = selected.getText().toString();
        }

        // Collect selected tags
        List<String> selectedTags = new ArrayList<>();
        if (tagTraffic.isChecked()) selectedTags.add("Heavy Traffic");
        if (tagWeather.isChecked()) selectedTags.add("Rainy/ Slippery Road");
        if (tagHelmet.isChecked()) selectedTags.add("Helmet Helped");

        // Timestamp & format data
        String tripId = ref.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // ====== Tambahan field date ======
        String tripDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        // ==================================

        long totalSeconds = tripDurationMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String formattedDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

        String formattedDistance = String.format(Locale.getDefault(), "%.2f km", distanceKm);
        String formattedSpeed = String.format(Locale.getDefault(), "%.1f km/h", avgSpeed);

        if (tripId != null) {
            Map<String, Object> tripData = new HashMap<>();
            tripData.put("tripId", tripId);
            tripData.put("timestamp", timestamp);
            tripData.put("date", tripDate); // <-- WAJIB UNTUK FILTER!
            tripData.put("duration", formattedDuration);
            tripData.put("distance", formattedDistance);
            tripData.put("status", "Ended");
            tripData.put("avgSpeed", formattedSpeed);

            // Structured notes
            Map<String, Object> structuredNote = new HashMap<>();
            structuredNote.put("mood", selectedMood);
            structuredNote.put("tags", selectedTags);
            structuredNote.put("text", noteText);
            tripData.put("notes", structuredNote);

            // Save path as 'path'
            if (routePoints != null) {
                List<Map<String, Double>> coords = new ArrayList<>();
                for (LatLng point : routePoints) {
                    Map<String, Double> latLngMap = new HashMap<>();
                    latLngMap.put("lat", point.latitude);
                    latLngMap.put("lng", point.longitude);
                    coords.add(latLngMap);
                }
                tripData.put("path", coords);
            }

            ref.child(tripId).setValue(tripData).addOnSuccessListener(task -> {
                Toast.makeText(getContext(), "✅ Trip saved successfully!", Toast.LENGTH_SHORT).show();
                redirectToHistory();
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "❌ Failed to save trip.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void discardTrip() {
        Toast.makeText(getContext(), "Trip discarded", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RiderStartTripFragment())
                .commit();
    }

    private void redirectToHistory() {
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, new RiderHistoryFragment());
        ft.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Show back the bottom nav safely
        if (getActivity() instanceof RiderHomeActivity) {
            ((RiderHomeActivity) getActivity()).setBottomNavVisibility(true);
        }
    }
}