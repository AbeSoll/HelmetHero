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

import java.text.SimpleDateFormat;
import java.util.*;

public class FamilyDashboardFragment extends Fragment {

    private RecyclerView recyclerRiderDashboard;
    private LinearLayout layoutNoRiders;  // Empty state layout
    private TextView textGreeting, textMotivation;
    private FamilyDashboardAdapter adapter;
    private final List<RiderDashboardData> riderDataList = new ArrayList<>();
    private DatabaseReference databaseRef;
    private final Map<String, ValueEventListener> activeListeners = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_dashboard, container, false);

        textGreeting = view.findViewById(R.id.textGreeting);
        textMotivation = view.findViewById(R.id.textMotivation);
        layoutNoRiders = view.findViewById(R.id.layoutNoRiders);
        recyclerRiderDashboard = view.findViewById(R.id.recyclerRiderDashboard);

        textGreeting.setText("Welcome 👋");
        textMotivation.setText("Monitor your riders' safety in real-time below.");

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
        toggleEmptyState(); // Initial check

        return view;
    }

    private String getGreeting() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        else if (hour < 18) return "Good afternoon";
        else return "Good evening";
    }

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
                subscribeToRiderData(linkedRiderUids);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void subscribeToRiderData(List<String> riderUids) {
        riderDataList.clear();
        adapter.notifyDataSetChanged();
        toggleEmptyState();

        if (riderUids.isEmpty()) {
            return;
        }

        for (String riderUid : riderUids) {
            // MAIN FIX: Use .exists() checks EVERYWHERE!
            ValueEventListener listener = databaseRef.child("Users").child(riderUid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            String riderName = userSnap.child("name").exists() ?
                                    userSnap.child("name").getValue(String.class) : "Unknown";
                            String profileUrl = userSnap.child("profileImageUrl").exists() ?
                                    userSnap.child("profileImageUrl").getValue(String.class) : null;

                            databaseRef.child("Riders").child(riderUid).child("liveTracking")
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot liveSnap) {
                                            boolean isActive = false;
                                            if (liveSnap.exists() && liveSnap.child("tripActive").exists()) {
                                                Boolean trip = liveSnap.child("tripActive").getValue(Boolean.class);
                                                isActive = trip != null && trip;
                                            }

                                            boolean finalIsActive = isActive;
                                            databaseRef.child("Trips").child(riderUid)
                                                    .orderByChild("timestamp").limitToLast(1)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot tripSnap) {
                                                            String lastTripSummary = "No trip yet";
                                                            for (DataSnapshot snap : tripSnap.getChildren()) {
                                                                String ts = snap.child("timestamp").exists() ?
                                                                        snap.child("timestamp").getValue(String.class) : null;
                                                                String distance = snap.child("distance").exists() ?
                                                                        snap.child("distance").getValue(String.class) : null;
                                                                String duration = snap.child("duration").exists() ?
                                                                        snap.child("duration").getValue(String.class) : null;

                                                                String formattedDate = formatToTarikh(ts);
                                                                String formattedTime = formatToMasaAMPM(ts);

                                                                lastTripSummary = "Last trip: " + formattedDate + " at " + formattedTime
                                                                        + "\nDistance: " + (distance != null ? distance : "-")
                                                                        + "\nDuration: " + (duration != null ? duration : "-");
                                                            }

                                                            boolean exists = false;
                                                            for (RiderDashboardData data : riderDataList) {
                                                                if (data.riderUid.equals(riderUid)) {
                                                                    data.name = riderName;
                                                                    data.profileImageUrl = profileUrl;
                                                                    data.activeRide = finalIsActive;
                                                                    data.lastTripSummary = lastTripSummary;
                                                                    exists = true;
                                                                    break;
                                                                }
                                                            }
                                                            if (!exists) {
                                                                RiderDashboardData data = new RiderDashboardData();
                                                                data.riderUid = riderUid;
                                                                data.name = riderName;
                                                                data.profileImageUrl = profileUrl;
                                                                data.activeRide = finalIsActive;
                                                                data.lastTripSummary = lastTripSummary;
                                                                riderDataList.add(data);
                                                            }
                                                            adapter.notifyDataSetChanged();
                                                            toggleEmptyState();
                                                        }
                                                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                                                    });
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                                    });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                    });

            activeListeners.put(riderUid, listener);
        }
    }

    private void toggleEmptyState() {
        if (layoutNoRiders == null || recyclerRiderDashboard == null) return;
        if (riderDataList.isEmpty()) {
            layoutNoRiders.setVisibility(View.VISIBLE);
            recyclerRiderDashboard.setVisibility(View.GONE);
        } else {
            layoutNoRiders.setVisibility(View.GONE);
            recyclerRiderDashboard.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseRef != null) {
            for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
                databaseRef.child("Users").child(entry.getKey()).removeEventListener(entry.getValue());
            }
        }
        activeListeners.clear();
    }

    private String formatToTarikh(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            Date date = input.parse(timestamp);
            return (date != null) ? output.format(date) : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private String formatToMasaAMPM(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = input.parse(timestamp);
            return (date != null) ? output.format(date) : "-";
        } catch (Exception e) {
            return "-";
        }
    }
}
