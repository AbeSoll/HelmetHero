package com.example.helmethero.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;

import com.example.helmethero.models.Alert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private static BluetoothService instance;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inStream;
    private Thread readThread;
    private boolean isConnected = false;
    private BluetoothListener listener;

    private static final String DEVICE_NAME = "HelmetHero-Helmet";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface BluetoothListener {
        void onStatusChanged(boolean connected);
        void onHelmetAlert(String message);
    }

    private BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothService getInstance() {
        if (instance == null) instance = new BluetoothService();
        return instance;
    }

    public void setListener(BluetoothListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return isConnected;
    }

    @SuppressLint("MissingPermission")
    public void connectHelmet() {
        if (bluetoothAdapter == null) return;

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        BluetoothDevice helmet = null;
        for (BluetoothDevice device : paired) {
            if (DEVICE_NAME.equals(device.getName())) {
                helmet = device;
                break;
            }
        }
        if (helmet == null) {
            isConnected = false;
            HelmetConnectionManager.setConnected(false);
            if (listener != null) listener.onStatusChanged(false);
            return;
        }

        BluetoothDevice finalHelmet = helmet;
        new Thread(() -> {
            try {
                socket = finalHelmet.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                inStream = socket.getInputStream();
                isConnected = true;
                HelmetConnectionManager.setConnected(true); // ✅ Update here
                if (listener != null) listener.onStatusChanged(true);
                startReading();
            } catch (IOException e) {
                isConnected = false;
                HelmetConnectionManager.setConnected(false); // ✅ Update here
                if (listener != null) listener.onStatusChanged(false);
                close();
            }
        }).start();
    }

    private void startReading() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[256];
            int bytes;
            StringBuilder sb = new StringBuilder();
            try {
                while (isConnected && inStream != null) {
                    if ((bytes = inStream.read(buffer)) > 0) {
                        String msg = new String(buffer, 0, bytes);
                        sb.append(msg);
                        int endIdx;
                        while ((endIdx = sb.indexOf("\n")) != -1) {
                            String alert = sb.substring(0, endIdx).trim();
                            sb.delete(0, endIdx + 1);
                            if (listener != null && !alert.isEmpty()) {
                                listener.onHelmetAlert(alert);
                            }
                            if (!alert.isEmpty() && alert.startsWith("ALERT:")) {
                                uploadAlertToFirebase(alert);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
                isConnected = false;
                HelmetConnectionManager.setConnected(false); // ✅ Update here
                if (listener != null) listener.onStatusChanged(false);
            }
        });
        readThread.start();
    }

    private void uploadAlertToFirebase(String msg) {
        if (!msg.startsWith("ALERT:")) return;

        String alertType = "", lat = "", lng = "";
        String[] parts = msg.split(";");
        for (String part : parts) {
            if (part.startsWith("ALERT:")) alertType = part.replace("ALERT:", "");
            if (part.startsWith("LAT:")) lat = part.replace("LAT:", "");
            if (part.startsWith("LNG:")) lng = part.replace("LNG:", "");
        }

        String timeNow = getCurrentTime();

        java.util.Map<String, Object> alertData = new java.util.HashMap<>();
        alertData.put("sosAlert", true);
        alertData.put("alert", alertType);
        alertData.put("alertTime", timeNow);
        alertData.put("location", lat + "," + lng);

        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Riders")
                .child(userUid)
                .child("liveTracking")
                .updateChildren(alertData);

        createFamilyAlertForHelmet(alertType, timeNow, lat + "," + lng);
    }

    private void createFamilyAlertForHelmet(String alertType, String alertTime, String location) {
        String riderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference riderRef = FirebaseDatabase.getInstance()
                .getReference("Riders").child(riderUid);

        riderRef.child("emergencyContacts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot contactSnap : snapshot.getChildren()) {
                    String familyUid = contactSnap.getKey();
                    if (familyUid == null) continue;

                    String alertId = FirebaseDatabase.getInstance().getReference("Alerts")
                            .child(familyUid).push().getKey();
                    if (alertId == null) continue;

                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("Users").child(riderUid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            String riderName = userSnap.child("name").getValue(String.class);
                            String profileImageUrl = userSnap.child("profileImageUrl").getValue(String.class);

                            Alert alert = new Alert(
                                    alertId, riderUid, riderName, profileImageUrl,
                                    alertType, alertTime, location, "NEW", false
                            );

                            FirebaseDatabase.getInstance()
                                    .getReference("Alerts")
                                    .child(familyUid)
                                    .child(alertId)
                                    .setValue(alert);
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    public void close() {
        try { if (inStream != null) inStream.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        isConnected = false;
        HelmetConnectionManager.setConnected(false); // ✅ Update here
        if (listener != null) listener.onStatusChanged(false);
    }
}