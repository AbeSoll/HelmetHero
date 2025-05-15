package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.models.Trip;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class RiderTripDetailFragment extends Fragment implements OnMapReadyCallback {

    private Trip trip;
    private EditText tripNotes;
    private TextView tripDate, tripDurationValue, tripDistanceValue, tripAvgSpeedValue;
    private GoogleMap map;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip_detail, container, false);

        // Bind views
        tripDate = view.findViewById(R.id.tripDate);
        tripDurationValue = view.findViewById(R.id.tripDurationValue);
        tripDistanceValue = view.findViewById(R.id.tripDistanceValue);
        tripAvgSpeedValue = view.findViewById(R.id.tripAvgSpeedValue);
        tripNotes = view.findViewById(R.id.tripNotes);
        Button btnSaveNote = view.findViewById(R.id.btnSaveNote);
        Button btnDeleteTrip = view.findViewById(R.id.btnDeleteTrip);

        // Map initialization
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.routeMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Get trip object from arguments
        if (getArguments() != null && getArguments().containsKey("trip")) {
            trip = (Trip) getArguments().getSerializable("trip");
            if (trip != null) {
                tripDate.setText(trip.getTimestamp());
                tripDurationValue.setText("Duration: " + trip.getDuration());
                tripDistanceValue.setText("Distance: " + trip.getDistance());
                tripAvgSpeedValue.setText("Average Speed: " + trip.getAvgSpeed());
                tripNotes.setText(trip.getNotes());
            }
        }

        // Save note logic
        btnSaveNote.setOnClickListener(v -> {
            String updatedNote = tripNotes.getText().toString().trim();
            if (trip != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Trips")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(trip.getTripId());

                ref.child("notes").setValue(updatedNote).addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "✅ Note updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Failed to update note", Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Delete trip logic
        btnDeleteTrip.setOnClickListener(v -> {
            if (trip != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Trips")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(trip.getTripId());

                ref.removeValue().addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "🗑️ Trip deleted", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Failed to delete trip", Toast.LENGTH_SHORT).show();
                });
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Load and draw route from Firebase
        if (trip != null) {
            DatabaseReference pathRef = FirebaseDatabase.getInstance()
                    .getReference("Trips")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(trip.getTripId())
                    .child("path");

            pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<LatLng> route = new ArrayList<>();
                    for (DataSnapshot pointSnapshot : snapshot.getChildren()) {
                        Double lat = pointSnapshot.child("lat").getValue(Double.class);
                        Double lng = pointSnapshot.child("lng").getValue(Double.class);
                        if (lat != null && lng != null) {
                            route.add(new LatLng(lat, lng));
                        }
                    }

                    if (!route.isEmpty()) {
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(route)
                                .width(8f)
                                .color(ContextCompat.getColor(requireContext(), R.color.helmet_blue))
                                .geodesic(true);
                        map.addPolyline(polylineOptions);

                        // Move camera to start point
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), 15f));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load route", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
