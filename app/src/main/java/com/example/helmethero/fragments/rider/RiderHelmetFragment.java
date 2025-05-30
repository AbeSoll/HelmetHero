package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;
import com.example.helmethero.utils.BluetoothService;

public class RiderHelmetFragment extends Fragment implements BluetoothService.BluetoothListener {

    private ImageView imgHelmetStatus;
    private TextView textHelmetStatus;
    private Button connectButton;

    @SuppressLint("NewApi")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_helmet, container, false);

        imgHelmetStatus = view.findViewById(R.id.imgHelmetStatus);
        textHelmetStatus = view.findViewById(R.id.textHelmetStatus);
        connectButton = view.findViewById(R.id.btnConnectHelmet);

        BluetoothService.getInstance().setListener(this);

        connectButton.setOnClickListener(v -> {
            textHelmetStatus.setText("Connecting...");
            imgHelmetStatus.setImageResource(R.drawable.ic_loading_spinner); // Optional spinner
            imgHelmetStatus.setTooltipText("Connecting...");
            BluetoothService.getInstance().connectHelmet();
        });

        // Set initial status
        updateHelmetUI(BluetoothService.getInstance().isConnected());

        return view;
    }

    @SuppressLint("NewApi")
    private void updateHelmetUI(boolean connected) {
        if (connected) {
            imgHelmetStatus.setImageResource(R.drawable.ic_success_green);
            imgHelmetStatus.setTooltipText("Helmet Connected");
            textHelmetStatus.setText("Helmet Connected");
        } else {
            imgHelmetStatus.setImageResource(R.drawable.ic_error_red);
            imgHelmetStatus.setTooltipText("Helmet Not Connected");
            textHelmetStatus.setText("Helmet Not Connected");
        }
    }

    @Override
    public void onStatusChanged(boolean connected) {
        requireActivity().runOnUiThread(() -> updateHelmetUI(connected));
    }

    @Override
    public void onHelmetAlert(String message) {
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), "Helmet Alert: " + message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BluetoothService.getInstance().setListener(null);
    }
}