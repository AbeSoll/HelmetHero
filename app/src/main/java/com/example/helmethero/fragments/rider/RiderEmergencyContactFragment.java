package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class RiderEmergencyContactFragment extends Fragment {

    private LinearLayout contactListLayout;
    private Button btnSave;
    private DatabaseReference usersRef, riderRef;
    private FirebaseUser currentUser;

    private ArrayList<String> selectedContacts = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_emergency_contact, container, false);

        contactListLayout = view.findViewById(R.id.contactListLayout);
        btnSave = view.findViewById(R.id.btnSaveContacts);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            usersRef = FirebaseDatabase.getInstance().getReference("Users");
            riderRef = FirebaseDatabase.getInstance().getReference("Riders")
                    .child(currentUser.getUid()).child("emergencyContacts");

            loadFamilyContacts();
        }

        btnSave.setOnClickListener(v -> saveSelectedContacts());

        return view;
    }

    private void loadFamilyContacts() {
        usersRef.orderByChild("role").equalTo("family").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactListLayout.removeAllViews();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String name = userSnap.child("name").getValue(String.class);
                    String uid = userSnap.getKey();

                    CheckBox checkBox = new CheckBox(getContext());
                    checkBox.setText(name);
                    checkBox.setTag(uid);

                    contactListLayout.addView(checkBox);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSelectedContacts() {
        selectedContacts.clear();
        for (int i = 0; i < contactListLayout.getChildCount(); i++) {
            View child = contactListLayout.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.isChecked()) {
                    selectedContacts.add(cb.getTag().toString());
                }
            }
        }

        if (selectedContacts.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one contact", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Boolean> map = new HashMap<>();
        for (String contactId : selectedContacts) {
            map.put(contactId, true);
        }

        riderRef.setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Contacts saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
