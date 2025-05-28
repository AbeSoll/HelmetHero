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

    private TextView helmetStatusText;
    private Button connectButton;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_helmet, container, false);

        helmetStatusText = view.findViewById(R.id.textHelmetStatus);
        connectButton = view.findViewById(R.id.btnConnectHelmet);

        // Register listener to get callback
        BluetoothService.getInstance().setListener(this);

        connectButton.setOnClickListener(v -> {
            helmetStatusText.setText("⏳ Connecting to helmet...");
            BluetoothService.getInstance().connectHelmet();
        });

        // Set initial status
        helmetStatusText.setText(
                BluetoothService.getInstance().isConnected() ?
                        "✅ Helmet Connected" :
                        "❌ Helmet Not Connected"
        );

        return view;
    }

    // BluetoothService.BluetoothListener
    @Override
    public void onStatusChanged(boolean connected) {
        requireActivity().runOnUiThread(() -> {
            helmetStatusText.setText(connected ? "✅ Helmet Connected" : "❌ Helmet Not Connected");
        });
    }

    @Override
    public void onHelmetAlert(String message) {
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), "Helmet Alert: " + message, Toast.LENGTH_SHORT).show()
        );

        // Optional: Call logic to upload alert to Firebase
        // e.g. handleHelmetAlert(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BluetoothService.getInstance().setListener(null); // avoid memory leak
    }
}
