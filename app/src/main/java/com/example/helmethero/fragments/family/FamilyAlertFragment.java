package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.helmethero.R;
import com.example.helmethero.adapters.AlertsAdapter;
import com.example.helmethero.models.Alert;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class FamilyAlertFragment extends Fragment {

    private RecyclerView recyclerAlerts;
    private TextView textNoAlerts;
    private final List<Alert> alertList = new ArrayList<>();
    private AlertsAdapter alertsAdapter;

    // For undo
    private Alert recentlyDeletedAlert = null;
    private int recentlyDeletedPosition = -1;
    private String recentlyDeletedAlertId = null;
    private Runnable pendingDeleteRunnable = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_alert, container, false);

        recyclerAlerts = view.findViewById(R.id.recyclerAlerts);
        textNoAlerts = view.findViewById(R.id.textNoAlerts);

        recyclerAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        alertsAdapter = new AlertsAdapter(alertList);
        recyclerAlerts.setAdapter(alertsAdapter);

        // Enable swipe-to-delete + undo
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        recentlyDeletedAlert = alertList.get(position);
                        recentlyDeletedPosition = position;
                        recentlyDeletedAlertId = recentlyDeletedAlert.getId();

                        // Remove from list immediately
                        alertList.remove(position);
                        alertsAdapter.notifyItemRemoved(position);

                        // Show Snackbar for Undo
                        Snackbar snackbar = Snackbar.make(recyclerAlerts, "Alert deleted", Snackbar.LENGTH_LONG);
                        snackbar.setAction("UNDO", v -> {
                            alertList.add(recentlyDeletedPosition, recentlyDeletedAlert);
                            alertsAdapter.notifyItemInserted(recentlyDeletedPosition);
                            recentlyDeletedAlert = null;
                            recentlyDeletedAlertId = null;
                            if (pendingDeleteRunnable != null) recyclerAlerts.removeCallbacks(pendingDeleteRunnable);
                        });

                        // If UNDO not clicked, after Snackbar timeout, remove from Firebase
                        pendingDeleteRunnable = () -> {
                            if (recentlyDeletedAlertId != null) {
                                String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("Alerts").child(familyUid);
                                alertsDbRef.child(recentlyDeletedAlertId).removeValue();
                                recentlyDeletedAlertId = null;
                            }
                        };
                        recyclerAlerts.postDelayed(pendingDeleteRunnable, 3500); // Snackbar.LENGTH_LONG = ~3.5s

                        snackbar.show();
                    }
                };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerAlerts);

        loadAllAlertsForAllLinkedRiders();

        return view;
    }

    // Fetch all linked riders and their alerts
    private void loadAllAlertsForAllLinkedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridersRef = FirebaseDatabase.getInstance().getReference("Riders");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

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
                if (linkedRiderUids.isEmpty()) {
                    alertList.clear();
                    alertsAdapter.notifyDataSetChanged();
                    textNoAlerts.setVisibility(View.VISIBLE);
                    textNoAlerts.setText("No linked riders.");
                    return;
                }
                fetchAllAlerts(linkedRiderUids);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textNoAlerts.setVisibility(View.VISIBLE);
                textNoAlerts.setText("Failed to load linked riders.");
            }
        });
    }

    // Fetch ALL alerts (history/archive) for all linked riders
    private void fetchAllAlerts(List<String> riderUids) {
        alertList.clear();
        alertsAdapter.notifyDataSetChanged();

        DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("Alerts");
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("Users");
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Use real-time listener so it always shows latest
        alertsDbRef.child(familyUid).addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alertList.clear();
                if (!snapshot.exists()) {
                    alertsAdapter.notifyDataSetChanged();
                    textNoAlerts.setVisibility(View.VISIBLE);
                    return;
                }
                List<Alert> tempAlerts = new ArrayList<>();
                List<DataSnapshot> allAlertSnaps = new ArrayList<>();
                for (DataSnapshot alertSnap : snapshot.getChildren()) {
                    allAlertSnaps.add(alertSnap);
                }
                final int[] completed = {0};
                if (allAlertSnaps.isEmpty()) {
                    alertsAdapter.notifyDataSetChanged();
                    textNoAlerts.setVisibility(View.VISIBLE);
                    return;
                }
                for (DataSnapshot alertSnap : allAlertSnaps) {
                    Alert alert = alertSnap.getValue(Alert.class);
                    if (alert == null) {
                        completed[0]++;
                        continue;
                    }
                    alert.setId(alertSnap.getKey()); // Make sure Alert model has .setId()
                    String alertRiderUid = alert.getRiderUid();
                    if (alertRiderUid == null || !riderUids.contains(alertRiderUid)) {
                        completed[0]++;
                        continue;
                    }
                    usersDbRef.child(alertRiderUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot riderSnap) {
                            String riderName = riderSnap.child("name").getValue(String.class);
                            String profileUrl = riderSnap.child("profileImageUrl").getValue(String.class);
                            alert.setRiderName(riderName);
                            alert.setProfileImageUrl(profileUrl);
                            tempAlerts.add(alert);

                            completed[0]++;
                            if (completed[0] == allAlertSnaps.size()) {
                                tempAlerts.sort((a, b) -> {
                                    String ta = a.getTime() != null ? a.getTime() : "";
                                    String tb = b.getTime() != null ? b.getTime() : "";
                                    return tb.compareTo(ta);
                                });
                                alertList.clear();
                                alertList.addAll(tempAlerts);
                                alertsAdapter.notifyDataSetChanged();
                                textNoAlerts.setVisibility(alertList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { completed[0]++; }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textNoAlerts.setVisibility(View.VISIBLE);
                textNoAlerts.setText("Failed to load alerts.");
            }
        });
    }
}