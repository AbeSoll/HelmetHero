package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FamilyAlertFragment extends Fragment implements AlertsAdapter.AlertSeenListener {

    private RecyclerView recyclerAlerts;
    private LinearLayout layoutNoAlerts;
    private final List<Alert> alertList = new ArrayList<>();
    private AlertsAdapter alertsAdapter;

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
        layoutNoAlerts = view.findViewById(R.id.layoutNoAlerts);

        recyclerAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass 'this' as the listener for the "Seen" button
        alertsAdapter = new AlertsAdapter(alertList, this);
        recyclerAlerts.setAdapter(alertsAdapter);

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

                        alertList.remove(position);
                        alertsAdapter.notifyItemRemoved(position);
                        toggleEmptyView();

                        Snackbar snackbar = Snackbar.make(recyclerAlerts, "Alert deleted", Snackbar.LENGTH_LONG);
                        snackbar.setAction("UNDO", v -> {
                            alertList.add(recentlyDeletedPosition, recentlyDeletedAlert);
                            alertsAdapter.notifyItemInserted(recentlyDeletedPosition);
                            toggleEmptyView();
                            recentlyDeletedAlert = null;
                            recentlyDeletedAlertId = null;
                            if (pendingDeleteRunnable != null) recyclerAlerts.removeCallbacks(pendingDeleteRunnable);
                        });

                        pendingDeleteRunnable = () -> {
                            if (recentlyDeletedAlertId != null) {
                                String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("Alerts").child(familyUid);
                                alertsDbRef.child(recentlyDeletedAlertId).removeValue();
                                recentlyDeletedAlertId = null;
                            }
                        };
                        recyclerAlerts.postDelayed(pendingDeleteRunnable, 3500);
                        snackbar.show();
                    }
                };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerAlerts);

        loadAllAlertsForAllLinkedRiders();

        return view;
    }

    private void loadAllAlertsForAllLinkedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridersRef = FirebaseDatabase.getInstance().getReference("Riders");

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
                    toggleEmptyView();
                    return;
                }
                fetchAllAlerts(linkedRiderUids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                alertList.clear();
                alertsAdapter.notifyDataSetChanged();
                toggleEmptyView();
            }
        });
    }

    private void toggleEmptyView() {
        if (alertList.isEmpty()) {
            layoutNoAlerts.setVisibility(View.VISIBLE);
            recyclerAlerts.setVisibility(View.GONE);
        } else {
            layoutNoAlerts.setVisibility(View.GONE);
            recyclerAlerts.setVisibility(View.VISIBLE);
        }
    }

    private void fetchAllAlerts(List<String> riderUids) {
        alertList.clear();
        alertsAdapter.notifyDataSetChanged();

        DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("Alerts");
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("Users");
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        alertsDbRef.child(familyUid).addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alertList.clear();
                if (!snapshot.exists()) {
                    alertsAdapter.notifyDataSetChanged();
                    toggleEmptyView();
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
                    toggleEmptyView();
                    return;
                }

                for (DataSnapshot alertSnap : allAlertSnaps) {
                    Alert alert = alertSnap.getValue(Alert.class);
                    if (alert == null) {
                        completed[0]++;
                        continue;
                    }

                    alert.setId(alertSnap.getKey());
                    String alertRiderUid = alert.getRiderUid();
                    if (alertRiderUid == null || !riderUids.contains(alertRiderUid)) {
                        completed[0]++;
                        continue;
                    }

                    // Format readable time
                    try {
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date parsed = originalFormat.parse(alert.getTime());
                        if (parsed != null) {
                            SimpleDateFormat displayFormat = new SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault());
                            alert.setFormattedTime(displayFormat.format(parsed));
                        } else {
                            alert.setFormattedTime(alert.getTime());
                        }
                    } catch (ParseException e) {
                        alert.setFormattedTime(alert.getTime());
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
                                toggleEmptyView();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            completed[0]++;
                            toggleEmptyView();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toggleEmptyView();
            }
        });
    }

    // ===== MARK ALERT AS SEEN LOGIC =====
    @Override
    public void onAlertSeen(String alertId) {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("Alerts")
                .child(familyUid)
                .child(alertId);
        alertRef.child("status").setValue("SEEN");
    }
}