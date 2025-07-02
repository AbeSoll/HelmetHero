package com.example.helmethero.services;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.helmethero.R;

public class LocationForegroundService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    public static final String CHANNEL_ID = "HelmetHeroLocationChannel";
    public static final int NOTIF_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startForeground(NOTIF_ID, getNotification("Trip in progress..."));

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted &= checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                granted &= checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }
        } else {
            granted &= checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        if (!granted) {
            stopSelf();
            return;
        }
        @SuppressLint("MissingPermission")
        // Recommended way using the Builder
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location location : result.getLocations()) {
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uid != null) {
                        DatabaseReference ref = FirebaseDatabase.getInstance()
                                .getReference("Riders").child(uid).child("liveTracking");
                        String locStr = location.getLatitude() + "," + location.getLongitude();
                        ref.child("location").setValue(locStr);
                        ref.child("lastUpdate").setValue(System.currentTimeMillis());
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service will not be killed by system unless memory is critically low
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // --- Helper Notification Channel ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "HelmetHero Location Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Runs trip tracking in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HelmetHero")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_helmethero_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }
}
