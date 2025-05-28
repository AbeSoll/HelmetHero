package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.adapters.LinkedRiderAdapter;
import com.example.helmethero.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class FamilyLinkedRidersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView textNoLinkedRiders;
    private LinkedRiderAdapter adapter;
    private final List<User> riderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_linked_riders, container, false);

        recyclerView = view.findViewById(R.id.recyclerLinkedRiders);
        textNoLinkedRiders = view.findViewById(R.id.textNoLinkedRiders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // ✅ Use the correct adapter constructor with a click listener!
        adapter = new LinkedRiderAdapter(riderList, rider -> {
            // Ensure UID is not null or empty
            if (rider.getUid() == null || rider.getUid().isEmpty()) {
                Toast.makeText(getContext(), "Rider UID not found!", Toast.LENGTH_SHORT).show();
                return;
            }
            Fragment tripListFragment = FamilyTripListFragment.newInstance(
                    rider.getUid(),
                    rider.getName()
            );
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.family_fragment_container, tripListFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);
        loadLinkedRiders();

        return view;
    }

    private void loadLinkedRiders() {
        String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridersRef = FirebaseDatabase.getInstance().getReference("Riders");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        ridersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                riderList.clear();

                List<String> linkedRiderUids = new ArrayList<>();
                for (DataSnapshot riderSnap : snapshot.getChildren()) {
                    DataSnapshot contactsSnap = riderSnap.child("emergencyContacts");
                    // **ONLY add if familyUid betul-betul ada dan value true!**
                    if (contactsSnap.hasChild(familyUid) && contactsSnap.child(familyUid).getValue(Boolean.class) != null
                            && Boolean.TRUE.equals(contactsSnap.child(familyUid).getValue(Boolean.class))) {
                        linkedRiderUids.add(riderSnap.getKey());
                    }
                }

                if (linkedRiderUids.isEmpty()) {
                    adapter.notifyDataSetChanged();
                    textNoLinkedRiders.setVisibility(View.VISIBLE);
                    textNoLinkedRiders.setText("No linked riders found.");
                    return;
                }

                // Now fetch user info from /Users/{riderUid} for each
                final int[] fetchCount = {0};
                for (String riderUid : linkedRiderUids) {
                    usersRef.child(riderUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            User user = userSnap.getValue(User.class);
                            if (user != null) {
                                user.setUid(userSnap.getKey());  // <- Must set UID here!
                                riderList.add(user);
                            }
                            fetchCount[0]++;
                            // Only notify after all riders fetched
                            if (fetchCount[0] == linkedRiderUids.size()) {
                                adapter.notifyDataSetChanged();
                                textNoLinkedRiders.setVisibility(riderList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            fetchCount[0]++;
                            if (fetchCount[0] == linkedRiderUids.size()) {
                                adapter.notifyDataSetChanged();
                                textNoLinkedRiders.setVisibility(riderList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textNoLinkedRiders.setText("Failed to load linked riders.");
                textNoLinkedRiders.setVisibility(View.VISIBLE);
            }
        });
    }
}
