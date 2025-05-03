package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.adapters.TripHistoryAdapter;
import com.example.helmethero.models.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RiderHistoryFragment extends Fragment {

    private TripHistoryAdapter adapter;
    private List<Trip> tripList;
    private DatabaseReference tripsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip_history, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerTripHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tripList = new ArrayList<>();
        adapter = new TripHistoryAdapter(tripList);
        recyclerView.setAdapter(adapter);

        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        tripsRef = FirebaseDatabase.getInstance().getReference("Trips").child(uid);

        loadTrips();

        return view;
    }

    private void loadTrips() {
        tripsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tripList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    if (trip != null) {
                        tripList.add(trip);
                    }
                }

                // ✅ Sort trips by timestamp descending (latest first)
                tripList.sort((t1, t2) -> {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        java.util.Date date1 = sdf.parse(t1.getTimestamp());
                        java.util.Date date2 = sdf.parse(t2.getTimestamp());
                        return date2.compareTo(date1); // latest first
                    } catch (Exception e) {
                        return 0;
                    }
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load trip history.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
