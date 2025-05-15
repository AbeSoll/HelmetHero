package com.example.helmethero.fragments.rider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.helmethero.R;

import com.example.helmethero.utils.HelmetConnectionManager;


public class RiderHelmetFragment extends Fragment {

    private TextView helmetStatusText;
    private BluetoothAdapter bluetoothAdapter;
    private static final String DEVICE_NAME = "HelmetHero-Helmet";
    private static final int REQUEST_BLUETOOTH_CONNECT = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_helmet, container, false);

        helmetStatusText = view.findViewById(R.id.textHelmetStatus);
        Button connectButton = view.findViewById(R.id.btnConnectHelmet);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectButton.setOnClickListener(v -> checkAndConnect());

        return view;
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void checkAndConnect() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBT);
            return;
        }

        // 🔐 Check Bluetooth permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return;
        }

        boolean isConnected = false;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (DEVICE_NAME.equals(device.getName())) {
                helmetStatusText.setText("✅ Helmet Connected");

                // ✅ Save status globally
                HelmetConnectionManager.setConnected(true);

                isConnected = true;
                break;
            }
        }

        if (!isConnected) {
            helmetStatusText.setText("❌ Helmet Not Connected");

            // ❌ Set global status false if not found
            HelmetConnectionManager.setConnected(false);

            Toast.makeText(getContext(), "Please pair the helmet via phone Bluetooth settings.", Toast.LENGTH_LONG).show();
        }
    }


    // 🎯 Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAndConnect(); // Retry connection check after permission granted
        } else {
            Toast.makeText(getContext(), "Bluetooth permission is required", Toast.LENGTH_SHORT).show();
        }
    }
}
