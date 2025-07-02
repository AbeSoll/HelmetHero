package com.example.helmethero.fragments.rider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.example.helmethero.utils.BluetoothService;

import java.util.ArrayList;
import java.util.List;

public class RiderHelmetFragment extends Fragment implements BluetoothService.BluetoothListener {

    private ImageView imgHelmetStatus;
    private TextView textHelmetStatus;
    private Button connectButton;

    // --- NEW: Launcher for requesting Bluetooth permissions ---
    private ActivityResultLauncher<String[]> bluetoothPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- NEW: Initialize the permission launcher ---
        bluetoothPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    // Check if all required permissions were granted
                    boolean allPermissionsGranted = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false) ||
                                !permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false)) {
                            allPermissionsGranted = false;
                        }
                    }

                    if (allPermissionsGranted) {
                        // Permissions granted, now we can try to connect.
                        proceedWithConnection();
                    } else {
                        // User denied permissions. Show a message and reset the UI.
                        Toast.makeText(getContext(), "Bluetooth permissions are required to connect.", Toast.LENGTH_LONG).show();
                        updateHelmetUI(false); // Reset to "Not Connected" state
                    }
                });
    }

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
            // --- UPDATED: Don't connect directly. Request permissions first. ---
            requestBluetoothPermissions();
        });

        // Set initial status from the service
        updateHelmetUI(BluetoothService.getInstance().isConnected());

        return view;
    }

    // --- NEW: Method to check and request permissions ---
    private void requestBluetoothPermissions() {
        // Show the "Connecting..." UI immediately for better user experience
        setConnectingUI();

        // For Android 12 (S) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> permissionsToRequest = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (!permissionsToRequest.isEmpty()) {
                // Launch the permission request dialog
                bluetoothPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            } else {
                // Permissions are already granted, proceed directly
                proceedWithConnection();
            }
        } else {
            // For Android 11 and below, permissions are granted in the manifest.
            proceedWithConnection();
        }
    }

    // --- NEW: Method to call the BluetoothService after permissions are handled ---
    private void proceedWithConnection() {
        // This is where the original button click logic now lives.
        BluetoothService.getInstance().connectHelmet();
    }

    @SuppressLint({"SetTextI18n"})
    private void updateHelmetUI(boolean connected) {
        if (getContext() == null) return; // Avoid crashes if fragment is detached
        if (connected) {
            imgHelmetStatus.setImageResource(R.drawable.ic_success_green);
            textHelmetStatus.setText("Helmet Connected");
        } else {
            imgHelmetStatus.setImageResource(R.drawable.ic_error_red);
            textHelmetStatus.setText("Helmet Not Connected");
        }
    }

    @SuppressLint({"SetTextI18n"})
    private void setConnectingUI() {
        if (getContext() == null) return;
        // Show the animated GIF using Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_loading_spinner)
                .into(imgHelmetStatus);
        textHelmetStatus.setText("Connecting...");
    }

    @Override
    public void onStatusChanged(boolean connected) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateHelmetUI(connected));
        }
    }

    @Override
    public void onHelmetAlert(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Helmet Alert: " + message, Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Important to avoid memory leaks
        BluetoothService.getInstance().setListener(null);
    }
}
