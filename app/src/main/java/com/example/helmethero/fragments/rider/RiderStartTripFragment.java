package com.example.helmethero.fragments.rider;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.activities.RiderHomeActivity;

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
        txtHelmetStatus = view.findViewById(R.id.textHelmetStatus);

        // Simulate connected helmet
        txtHelmetStatus.setText("🟢 Helmet Connected");

        btnStartTrip.setOnClickListener(v -> {
            // Navigate to RiderTripFragment (in-progress)
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RiderTripFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
