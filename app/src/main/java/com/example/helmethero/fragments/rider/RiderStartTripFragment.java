package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.utils.HelmetConnectionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class RiderStartTripFragment extends Fragment {

    private Button btnStartTrip;
    private TextView txtHelmetStatus, txtWelcome;
    private ImageView imgHelmetStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_rider_start_trip, container, false);

        btnStartTrip = view.findViewById(R.id.btnStartTrip);
        txtHelmetStatus = view.findViewById(R.id.textHelmetStatus);
        txtWelcome = view.findViewById(R.id.textWelcome);
        imgHelmetStatus = view.findViewById(R.id.imgHelmetStatus);

        // Dapatkan nama pengguna dari Firebase
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    txtWelcome.setText("Hello, " + name + " 👋");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtWelcome.setText("Hello, Rider 👋");
            }
        });

        btnStartTrip.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RiderTripFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHelmetStatus();
    }

    @SuppressLint({"SetTextI18n", "NewApi"})
    private void updateHelmetStatus() {
        boolean isConnected = HelmetConnectionManager.isConnected();

        txtHelmetStatus.setText(isConnected ? "Helmet Connected" : "Helmet Not Connected");
        imgHelmetStatus.setImageResource(isConnected ? R.drawable.ic_success_green : R.drawable.ic_error_red);
        imgHelmetStatus.setTooltipText(isConnected ? "Helmet successfully connected" : "Helmet connection not detected");
    }
}
