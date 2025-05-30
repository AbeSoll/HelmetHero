package com.example.helmethero.fragments.family;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.models.Trip;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FamilyTripDetailFragment extends Fragment implements OnMapReadyCallback {

    private Trip trip;
    private EditText editTripNote;
    private TextView tripDate, tripDurationValue, tripDistanceValue, tripAvgSpeedValue;
    private GoogleMap map;
    private RadioGroup radioMoodGroup;
    private CheckBox tagTraffic, tagWeather, tagHelmet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip_detail, container, false);

        tripDate = view.findViewById(R.id.tripDate);
        tripDurationValue = view.findViewById(R.id.tripDurationValue);
        tripDistanceValue = view.findViewById(R.id.tripDistanceValue);
        tripAvgSpeedValue = view.findViewById(R.id.tripAvgSpeedValue);

        editTripNote = view.findViewById(R.id.editTripNote);
        radioMoodGroup = view.findViewById(R.id.radioMoodGroup);
        tagTraffic = view.findViewById(R.id.tag_traffic);
        tagWeather = view.findViewById(R.id.tag_weather);
        tagHelmet = view.findViewById(R.id.tag_helmet);

        editTripNote.setEnabled(false);
        for (int i = 0; i < radioMoodGroup.getChildCount(); i++) {
            radioMoodGroup.getChildAt(i).setEnabled(false);
        }
        tagTraffic.setEnabled(false);
        tagWeather.setEnabled(false);
        tagHelmet.setEnabled(false);

        Button btnSaveNote = view.findViewById(R.id.btnSaveNote);
        Button btnDeleteTrip = view.findViewById(R.id.btnDeleteTrip);
        if (btnSaveNote != null) btnSaveNote.setVisibility(View.GONE);
        if (btnDeleteTrip != null) btnDeleteTrip.setVisibility(View.GONE);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.routeMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (getArguments() != null) {
            trip = (Trip) getArguments().getSerializable("trip");
            String riderUid = getArguments().getString("riderUid", "");
            if (trip != null) {
                // === FORMAT TARIKH ===
                String formattedDate = trip.getTimestamp(); // fallback
                try {
                    SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date parsedDate = originalFormat.parse(trip.getTimestamp());
                    if (parsedDate != null) {
                        SimpleDateFormat displayFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                        formattedDate = displayFormat.format(parsedDate);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                tripDate.setText("Date: " + formattedDate);

                tripDurationValue.setText("Duration: " + trip.getDuration());
                tripDistanceValue.setText("Distance: " + trip.getDistance());
                tripAvgSpeedValue.setText("Average Speed: " + trip.getAvgSpeed());

                DatabaseReference tripRef = FirebaseDatabase.getInstance()
                        .getReference("Trips")
                        .child(riderUid)
                        .child(trip.getTripId());

                tripRef.child("notes").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String mood = snapshot.child("mood").getValue(String.class);
                            if (mood != null) {
                                switch (mood) {
                                    case "😄": radioMoodGroup.check(R.id.mood_happy); break;
                                    case "😌": radioMoodGroup.check(R.id.mood_calm); break;
                                    case "😰": radioMoodGroup.check(R.id.mood_stressed); break;
                                    case "⚠️": radioMoodGroup.check(R.id.mood_alert); break;
                                }
                            }
                            for (DataSnapshot tagSnap : snapshot.child("tags").getChildren()) {
                                String tag = tagSnap.getValue(String.class);
                                if (tag != null) {
                                    if (tag.equals("Heavy Traffic")) tagTraffic.setChecked(true);
                                    if (tag.equals("Rainy/ Slippery Road")) tagWeather.setChecked(true);
                                    if (tag.equals("Helmet Helped")) tagHelmet.setChecked(true);
                                }
                            }
                            String noteText = snapshot.child("text").getValue(String.class);
                            editTripNote.setText(noteText);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { /* ignore */ }
                });
            }
        }
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        if (trip != null) {
            String riderUid = getArguments().getString("riderUid", "");
            DatabaseReference pathRef = FirebaseDatabase.getInstance()
                    .getReference("Trips")
                    .child(riderUid)
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
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), 15f));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    public static FamilyTripDetailFragment newInstance(Object trip, String riderUid) {
        FamilyTripDetailFragment fragment = new FamilyTripDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("trip", (Serializable) trip);
        args.putString("riderUid", riderUid);
        fragment.setArguments(args);
        return fragment;
    }
}