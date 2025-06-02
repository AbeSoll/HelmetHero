package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.activities.QrScanActivity;
import com.example.helmethero.adapters.LinkedRiderAdapter;
import com.example.helmethero.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class FamilyLinkedRidersFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout layoutNoLinkedRiders;
    private Button btnAddRider, btnUnlinkSelected;
    private LinkedRiderAdapter adapter;
    private final List<User> riderList = new ArrayList<>();
    private final Set<String> selectedUids = new HashSet<>();

    private static final int QR_SCAN_REQUEST = 1102;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_linked_riders, container, false);

        recyclerView = view.findViewById(R.id.recyclerLinkedRiders);
        layoutNoLinkedRiders = view.findViewById(R.id.layoutNoLinkedRiders);
        btnAddRider = view.findViewById(R.id.btnAddRider);
        btnUnlinkSelected = view.findViewById(R.id.btnUnlinkSelected);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LinkedRiderAdapter(riderList, selectedUids, () -> {
            btnUnlinkSelected.setVisibility(selectedUids.isEmpty() ? View.GONE : View.VISIBLE);
        });

        recyclerView.setAdapter(adapter);
        btnAddRider.setOnClickListener(v -> showAddRiderDialog());
        btnUnlinkSelected.setOnClickListener(v -> confirmUnlinkSelected());

        // Click open trip history
        adapter.setOnRiderItemClickListener(rider -> {
            FamilyTripListFragment tripFragment = FamilyTripListFragment.newInstance(rider.getUid(), rider.getName());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.family_fragment_container, tripFragment)
                    .addToBackStack(null)
                    .commit();
        });

        loadLinkedRiders();
        return view;
    }

    private void confirmUnlinkSelected() {
        if (selectedUids.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Unlink Riders")
                .setMessage("Are you sure you want to unlink selected rider(s)?")
                .setPositiveButton("Yes", (dialog, which) -> unlinkSelectedRiders())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unlinkSelectedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (String riderUid : selectedUids) {
            FirebaseDatabase.getInstance()
                    .getReference("Riders").child(riderUid).child("emergencyContacts")
                    .child(familyUid).removeValue();

            FirebaseDatabase.getInstance()
                    .getReference("FamilyContacts").child(familyUid).child("linkedRiders")
                    .child(riderUid).removeValue();

            riderList.removeIf(r -> Objects.equals(r.getUid(), riderUid));
        }

        // WAJIB: clear selection & update UI
        selectedUids.clear();
        adapter.notifyDataSetChanged();
        btnUnlinkSelected.setVisibility(View.GONE);
        toggleEmptyState();
        Toast.makeText(getContext(), "Selected riders unlinked successfully.", Toast.LENGTH_SHORT).show();
    }

    private void toggleEmptyState() {
        if (riderList.isEmpty()) {
            layoutNoLinkedRiders.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutNoLinkedRiders.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddRiderDialog() {
        // Modern look dialog with chip-like tab for code/QR
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(36, 48, 36, 18);

        // Tab bar using TextViews as chips
        LinearLayout tabBar = new LinearLayout(requireContext());
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(0, 0, 0, 28);

        TextView btnTabCode = new TextView(requireContext());
        btnTabCode.setText("Code");
        btnTabCode.setTypeface(null, Typeface.BOLD);
        btnTabCode.setGravity(Gravity.CENTER);
        btnTabCode.setTextSize(16);
        btnTabCode.setBackgroundResource(R.drawable.tab_selector);
        btnTabCode.setTextColor(Color.WHITE);
        btnTabCode.setSelected(true);

        TextView btnTabQR = new TextView(requireContext());
        btnTabQR.setText("QR");
        btnTabQR.setTypeface(null, Typeface.BOLD);
        btnTabQR.setGravity(Gravity.CENTER);
        btnTabQR.setTextSize(16);
        btnTabQR.setBackgroundResource(R.drawable.tab_selector);
        btnTabQR.setTextColor(Color.parseColor("#1E2D60"));
        btnTabQR.setSelected(false);

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, 88, 1f);
        tabParams.setMargins(8, 0, 8, 0);
        tabBar.addView(btnTabCode, tabParams);
        tabBar.addView(btnTabQR, tabParams);
        dialogLayout.addView(tabBar);

        // Code layout
        LinearLayout codeLayout = new LinearLayout(requireContext());
        codeLayout.setOrientation(LinearLayout.VERTICAL);
        codeLayout.setPadding(0, 20, 0, 0);

        EditText codeInput = new EditText(requireContext());
        codeInput.setHint("Rider Invite Code");
        codeInput.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeParams.setMargins(0, 0, 0, 22); // Add space after EditText
        codeInput.setLayoutParams(codeParams);

        Button btnLink = new Button(requireContext());
        btnLink.setText("Add Rider");
        btnLink.setAllCaps(false);
        btnLink.setBackgroundResource(R.drawable.rounded_button_blue); // use your preferred style
        btnLink.setTextColor(0xFFFFFFFF);

        codeLayout.addView(codeInput);
        codeLayout.addView(btnLink);

        // QR layout
        LinearLayout qrLayout = new LinearLayout(requireContext());
        qrLayout.setOrientation(LinearLayout.VERTICAL);
        qrLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        qrLayout.setPadding(0, 20, 0, 0);

        Button btnScan = new Button(requireContext());
        btnScan.setText("Scan QR");
        btnScan.setAllCaps(false);
        btnScan.setBackgroundResource(R.drawable.rounded_button_blue);
        btnScan.setTextColor(Color.WHITE);

        TextView qrHint = new TextView(requireContext());
        qrHint.setText("Ask rider to show their QR code.");
        qrHint.setPadding(0, 30, 0, 0);
        qrHint.setTextSize(15);
        qrHint.setGravity(Gravity.CENTER);

        qrLayout.addView(btnScan);
        qrLayout.addView(qrHint);

        // FrameLayout for switching between code/QR
        FrameLayout contentFrame = new FrameLayout(requireContext());
        contentFrame.addView(codeLayout);
        contentFrame.addView(qrLayout);
        qrLayout.setVisibility(View.GONE);

        dialogLayout.addView(contentFrame);

        // Tab switching logic
        btnTabCode.setOnClickListener(v -> {
            btnTabCode.setSelected(true);
            btnTabCode.setTextColor(Color.WHITE);
            btnTabQR.setSelected(false);
            btnTabQR.setTextColor(Color.parseColor("#1E2D60"));
            codeLayout.setVisibility(View.VISIBLE);
            qrLayout.setVisibility(View.GONE);
        });
        btnTabQR.setOnClickListener(v -> {
            btnTabQR.setSelected(true);
            btnTabQR.setTextColor(Color.WHITE);
            btnTabCode.setSelected(false);
            btnTabCode.setTextColor(Color.parseColor("#1E2D60"));
            codeLayout.setVisibility(View.GONE);
            qrLayout.setVisibility(View.VISIBLE);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Rider")
                .setView(dialogLayout)
                .setNegativeButton("Cancel", null)
                .create();

        btnLink.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                linkWithRider(code, dialog);
            } else {
                codeInput.setError("Please enter invite code");
            }
        });

        btnScan.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(requireContext(), QrScanActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST);
        });

        dialog.show();
    }

    private void linkWithRider(String riderUid, AlertDialog dialogToClose) {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(riderUid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DatabaseReference riderEmergencyContacts = FirebaseDatabase.getInstance()
                            .getReference("Riders").child(riderUid).child("emergencyContacts");
                    riderEmergencyContacts.child(familyUid).setValue(true);

                    Toast.makeText(getContext(), "Successfully linked to rider!", Toast.LENGTH_SHORT).show();
                    if (dialogToClose != null) dialogToClose.dismiss();
                    loadLinkedRiders(); // reload list & clear selection
                } else {
                    Toast.makeText(getContext(), "Rider not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            String scannedUid = data.getStringExtra("QR_RESULT");
            if (scannedUid != null && !scannedUid.isEmpty()) {
                linkWithRider(scannedUid, null);
            } else {
                Toast.makeText(getContext(), "QR scan failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadLinkedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridersRef = FirebaseDatabase.getInstance().getReference("Riders");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        riderList.clear();
        selectedUids.clear();
        btnUnlinkSelected.setVisibility(View.GONE); // Hide button on reload

        ridersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> linkedRiderUids = new ArrayList<>();
                for (DataSnapshot riderSnap : snapshot.getChildren()) {
                    DataSnapshot contactsSnap = riderSnap.child("emergencyContacts");
                    if (contactsSnap.hasChild(familyUid) && Boolean.TRUE.equals(contactsSnap.child(familyUid).getValue(Boolean.class))) {
                        linkedRiderUids.add(riderSnap.getKey());
                    }
                }

                if (linkedRiderUids.isEmpty()) {
                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    return;
                }

                final int[] fetchCount = {0};
                for (String riderUid : linkedRiderUids) {
                    usersRef.child(riderUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            User user = userSnap.getValue(User.class);
                            if (user != null) {
                                user.setUid(userSnap.getKey());
                                riderList.add(user);
                            }
                            fetchCount[0]++;
                            if (fetchCount[0] == linkedRiderUids.size()) {
                                adapter.notifyDataSetChanged();
                                toggleEmptyState();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            fetchCount[0]++;
                            if (fetchCount[0] == linkedRiderUids.size()) {
                                adapter.notifyDataSetChanged();
                                toggleEmptyState();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toggleEmptyState();
            }
        });
    }
}