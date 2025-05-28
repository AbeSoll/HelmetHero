package com.example.helmethero.fragments.family;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.helmethero.R;
import com.example.helmethero.models.RiderDashboardData;
import com.example.helmethero.adapters.FamilyDashboardAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class FamilyDashboardFragment extends Fragment {

    private RecyclerView recyclerRiderDashboard;
    private TextView textNoRiders;
    private TextView textGreeting, textMotivation;
    private FamilyDashboardAdapter adapter;
    private final List<RiderDashboardData> riderDataList = new ArrayList<>();
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_dashboard, container, false);

        // NEW: Greeting & motivational text
        textGreeting = view.findViewById(R.id.textGreeting);
        textMotivation = view.findViewById(R.id.textMotivation);

        // Set default values first
        textGreeting.setText("Welcome 👋");
        textMotivation.setText("Monitor your riders' safety in real-time below.");

        // Fetch family user's name for greeting
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userUid);
        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                String greeting = getGreeting() + (name != null ? ", " + name + " 👋" : " 👋");
                textGreeting.setText(greeting);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textGreeting.setText("Welcome 👋");
            }
        });

        recyclerRiderDashboard = view.findViewById(R.id.recyclerRiderDashboard);
        textNoRiders = view.findViewById(R.id.textNoRiders);

        recyclerRiderDashboard.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FamilyDashboardAdapter(riderDataList, getContext());
        recyclerRiderDashboard.setAdapter(adapter);

        adapter.setOnRiderCardActionListener(new FamilyDashboardAdapter.OnRiderCardActionListener() {
            @Override
            public void onViewLive(RiderDashboardData rider) {
                Bundle args = new Bundle();
                args.putString("riderUid", rider.riderUid);
                FamilyRealTimeFragment fragment = new FamilyRealTimeFragment();
                fragment.setArguments(args);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.family_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onViewHistory(RiderDashboardData rider) {
                if (rider.riderUid == null || rider.riderUid.isEmpty()) {
                    Toast.makeText(getContext(), "No rider UID found!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Fragment fragment = FamilyTripListFragment.newInstance(
                        rider.riderUid,
                        rider.name != null ? rider.name : ""
                );
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.family_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onViewAlerts(RiderDashboardData rider) {
                Bundle args = new Bundle();
                args.putString("riderUid", rider.riderUid);
                FamilyAlertFragment fragment = new FamilyAlertFragment();
                fragment.setArguments(args);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.family_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        fetchAllLinkedRiders();
        return view;
    }

    // Time-based greeting helper
    private String getGreeting() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        else if (hour < 18) return "Good afternoon";
        else return "Good evening";
    }

    // === NEW: Only show riders who have this familyUid as emergency contact ===
    private void fetchAllLinkedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        DatabaseReference ridersRef = databaseRef.child("Riders");
        ridersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> linkedRiderUids = new ArrayList<>();
                for (DataSnapshot riderSnap : snapshot.getChildren()) {
                    DataSnapshot contactsSnap = riderSnap.child("emergencyContacts");
                    if (contactsSnap.child(familyUid).exists()) {
                        linkedRiderUids.add(riderSnap.getKey());
                    }
                }
                fetchRiderData(linkedRiderUids);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void fetchRiderData(List<String> riderUids) {
        riderDataList.clear();
        adapter.notifyDataSetChanged();
        if (riderUids.isEmpty()) {
            textNoRiders.setVisibility(View.VISIBLE);
            return;
        } else {
            textNoRiders.setVisibility(View.GONE);
        }

        for (String riderUid : riderUids) {
            // Fetch user, liveTracking, and last trip
            databaseRef.child("Users").child(riderUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            String riderName = userSnap.child("name").getValue(String.class);
                            String profileUrl = userSnap.child("profileImageUrl").getValue(String.class);

                            databaseRef.child("Riders").child(riderUid).child("liveTracking")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot liveSnap) {
                                            boolean isActive = liveSnap.child("tripActive").getValue(Boolean.class) != null
                                                    && liveSnap.child("tripActive").getValue(Boolean.class);

                                            databaseRef.child("Trips").child(riderUid)
                                                    .orderByChild("timestamp").limitToLast(1)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot tripSnap) {
                                                            String lastTripSummary = "No trip yet";
                                                            for (DataSnapshot snap : tripSnap.getChildren()) {
                                                                String ts = snap.child("timestamp").getValue(String.class);
                                                                String distance = snap.child("distance").getValue(String.class);
                                                                String duration = snap.child("duration").getValue(String.class);
                                                                lastTripSummary = "Last trip: " + (ts != null ? ts : "-")
                                                                        + " • " + (distance != null ? distance : "-")
                                                                        + " • " + (duration != null ? duration : "-");
                                                            }

                                                            RiderDashboardData data = new RiderDashboardData();
                                                            data.riderUid = riderUid;
                                                            data.name = riderName;
                                                            data.profileImageUrl = profileUrl;
                                                            data.activeRide = isActive;
                                                            data.lastTripSummary = lastTripSummary;
                                                            riderDataList.add(data);
                                                            adapter.notifyDataSetChanged();
                                                        }
                                                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                                                    });
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                                    });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                    });
        }
    }
}