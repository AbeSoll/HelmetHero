package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.adapters.EmergencyContactAdapter;
import com.example.helmethero.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class RiderEmergencyContactFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout layoutContent, layoutNoContacts;
    private Button btnUnlinkSelected;
    private EmergencyContactAdapter adapter;

    private final List<User> contactList = new ArrayList<>();
    private final Set<String> selectedUids = new HashSet<>();
    private DatabaseReference usersRef, riderRef, familyContactRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_emergency_contact, container, false);

        recyclerView = view.findViewById(R.id.recyclerEmergencyContacts);
        layoutContent = view.findViewById(R.id.layoutContent);
        layoutNoContacts = view.findViewById(R.id.layoutNoContacts);
        btnUnlinkSelected = view.findViewById(R.id.btnUnlinkSelected);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EmergencyContactAdapter(contactList, selectedUids, () -> {
            btnUnlinkSelected.setVisibility(selectedUids.isEmpty() ? View.GONE : View.VISIBLE);
        });
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            usersRef = FirebaseDatabase.getInstance().getReference("Users");
            riderRef = FirebaseDatabase.getInstance().getReference("Riders")
                    .child(currentUser.getUid()).child("emergencyContacts");
            familyContactRef = FirebaseDatabase.getInstance().getReference("FamilyContacts");

            loadEmergencyContacts();
        }

        btnUnlinkSelected.setOnClickListener(v -> confirmUnlinkSelected());
        return view;
    }

    private void confirmUnlinkSelected() {
        if (selectedUids.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Unlink Contacts")
                .setMessage("Are you sure you want to unlink the selected contacts?")
                .setPositiveButton("Yes", (dialog, which) -> unlinkSelectedContacts())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void unlinkSelectedContacts() {
        String riderUid = currentUser.getUid();

        for (String familyUid : selectedUids) {
            riderRef.child(familyUid).removeValue();
            familyContactRef.child(familyUid).child("linkedRiders").child(riderUid).removeValue();

            contactList.removeIf(u -> u.getUid().equals(familyUid));
        }

        selectedUids.clear();
        adapter.notifyDataSetChanged();
        btnUnlinkSelected.setVisibility(View.GONE);
        toggleEmptyState(contactList.isEmpty());
        Toast.makeText(getContext(), "Contacts unlinked successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadEmergencyContacts() {
        contactList.clear();
        selectedUids.clear();
        btnUnlinkSelected.setVisibility(View.GONE);

        riderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot contactsSnap) {
                List<String> linkedFamilyUids = new ArrayList<>();
                for (DataSnapshot snap : contactsSnap.getChildren()) {
                    if (Boolean.TRUE.equals(snap.getValue(Boolean.class))) {
                        linkedFamilyUids.add(snap.getKey());
                    }
                }

                if (linkedFamilyUids.isEmpty()) {
                    toggleEmptyState(true);
                    adapter.notifyDataSetChanged();
                    return;
                } else {
                    toggleEmptyState(false);
                }

                final int[] fetchCount = {0};
                for (String familyUid : linkedFamilyUids) {
                    usersRef.child(familyUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            if (userSnap.exists()) {
                                String name = userSnap.child("name").getValue(String.class);
                                String email = userSnap.child("email").getValue(String.class);
                                String phone = userSnap.child("phone").getValue(String.class);
                                String profileImageUrl = userSnap.child("profileImageUrl").getValue(String.class);
                                contactList.add(new User(familyUid, name, email, phone, profileImageUrl));
                            }
                            fetchCount[0]++;
                            if (fetchCount[0] == linkedFamilyUids.size()) {
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            fetchCount[0]++;
                            if (fetchCount[0] == linkedFamilyUids.size()) {
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toggleEmptyState(true);
                adapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleEmptyState(boolean showEmpty) {
        layoutContent.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        layoutNoContacts.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }
}