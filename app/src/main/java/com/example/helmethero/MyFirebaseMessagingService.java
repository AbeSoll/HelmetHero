package com.example.helmethero;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken");
            ref.setValue(token);
            // Log and Toast for debugging
            android.util.Log.d("MyFirebaseMsgService", "FCM Token updated: " + token);
            android.os.Handler handler = new android.os.Handler(getMainLooper());
            handler.post(() -> android.widget.Toast.makeText(getApplicationContext(), "Token updated: " + token, android.widget.Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Default values
        String title = "HelmetHero Alert";
        String message = "You have a new notification!";
        String googleMapsUrl = null;

        // Always prioritize data payload (for custom FCM)
        if (remoteMessage.getData() != null) {
            if (remoteMessage.getData().containsKey("riderName")) {
                String riderName = remoteMessage.getData().get("riderName");
                title = "SOS: " + riderName;
                message = riderName + " has triggered an SOS alert! Tap to view location.";
            }
            if (remoteMessage.getData().containsKey("googleMapsUrl")) {
                googleMapsUrl = remoteMessage.getData().get("googleMapsUrl");
            }
        }

        // Optional fallback: use notification payload if data is missing
        if (remoteMessage.getNotification() != null) {
            if (title.equals("HelmetHero Alert") && remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (message.equals("You have a new notification!") && remoteMessage.getNotification().getBody() != null) {
                message = remoteMessage.getNotification().getBody();
            }
        }

        showNotification(title, message, googleMapsUrl);
    }

    private void showNotification(String title, String message, String googleMapsUrl) {
        Intent intent;
        if (googleMapsUrl != null && !googleMapsUrl.isEmpty()) {
            // Direct to Google Maps if present
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsUrl));
        } else {
            // Fallback: open app home
            intent = new Intent(this, com.example.helmethero.activities.FamilyHomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "helmethero_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_helmethero_logo)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "HelmetHero Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0, notificationBuilder.build());
    }
}
