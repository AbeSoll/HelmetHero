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
            android.util.Log.d("MyFirebaseMsgService", "FCM Token updated: " + token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Dynamic title/message from FCM payload
        String title = "HelmetHero";
        String message = "You have a new notification!";
        String googleMapsUrl = null;

        // 1. Prefer Data payload if present
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                message = remoteMessage.getData().get("body");
            }
            if (remoteMessage.getData().containsKey("googleMapsUrl")) {
                googleMapsUrl = remoteMessage.getData().get("googleMapsUrl");
            }
            // Fallback for previous function style: if riderName exists, update title/message
            if (remoteMessage.getData().containsKey("riderName")) {
                String riderName = remoteMessage.getData().get("riderName");
                if (remoteMessage.getData().containsKey("sosType")) {
                    title = "SOS: " + riderName;
                    message = riderName + " has triggered an SOS alert! Tap to view location.";
                }
            }
            // Trip start/end, link/unlink can just use title/body sent from backend
        }

        // 2. Use notification payload (if any) if not overridden above
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
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