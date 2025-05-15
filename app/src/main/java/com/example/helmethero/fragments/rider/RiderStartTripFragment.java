package com.example.helmethero.fragments.rider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.utils.HelmetConnectionManager;

public class RiderStartTripFragment extends Fragment {

    private Button btnStartTrip;
    private TextView txtHelmetStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_rider_start_trip, container, false);

        btnStartTrip = view.findViewById(R.id.btnStartTrip);
        txtHelmetStatus = view.findViewById(R.id.textHelmetStatus); // Store reference for later use

        btnStartTrip.setOnClickListener(v -> {
            // Navigate to RiderTripFragment
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

    private void updateHelmetStatus() {
        if (HelmetConnectionManager.isConnected()) {
            txtHelmetStatus.setText("✅ Helmet Connected");
        } else {
            txtHelmetStatus.setText("❌ Helmet Not Connected");
        }
    }
}
