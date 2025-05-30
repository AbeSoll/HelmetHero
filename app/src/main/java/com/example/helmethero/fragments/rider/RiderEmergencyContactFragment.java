package com.example.helmethero.fragments.rider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
    private Button btnSave;
    private EmergencyContactAdapter adapter;

    private final List<User> contactList = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>(); // Current selection
    private final Set<String> previousSelectedIds = new HashSet<>(); // Previously saved selection

    private DatabaseReference usersRef, riderRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_emergency_contact, container, false);

        recyclerView = view.findViewById(R.id.recyclerEmergencyContacts);
        btnSave = view.findViewById(R.id.btnSaveContacts);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EmergencyContactAdapter(contactList, selectedIds);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            usersRef = FirebaseDatabase.getInstance().getReference("Users");
            riderRef = FirebaseDatabase.getInstance().getReference("Riders")
                    .child(currentUser.getUid()).child("emergencyContacts");

            loadContactsAndRestoreSelection();
        }

        btnSave.setOnClickListener(v -> saveSelectedContacts());

        return view;
    }

    private void loadContactsAndRestoreSelection() {
        usersRef.orderByChild("role").equalTo("Family Member").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    String name = userSnap.child("name").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String phone = userSnap.child("phone").getValue(String.class); // or "phoneNumber" if that's the key
                    String profileImageUrl = userSnap.child("profileImageUrl").getValue(String.class);

                    contactList.add(new User(uid, name, email, phone, profileImageUrl));
                }

                // Restore previously selected contacts
                riderRef.get().addOnSuccessListener(dataSnapshot -> {
                    selectedIds.clear();
                    previousSelectedIds.clear();
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        selectedIds.add(snap.getKey());
                        previousSelectedIds.add(snap.getKey());
                    }
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void saveSelectedContacts() {
        // Allow saving even if no family is selected (unlink all)
        HashMap<String, Boolean> map = new HashMap<>();
        for (String contactId : selectedIds) {
            map.put(contactId, true);
        }

        riderRef.setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                previousSelectedIds.clear();
                previousSelectedIds.addAll(selectedIds);

                if (selectedIds.isEmpty()) {
                    Toast.makeText(getContext(), "All family contacts have been unlinked.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Contacts saved successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Failed to save contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
