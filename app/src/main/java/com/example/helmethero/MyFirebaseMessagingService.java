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

import com.example.helmethero.activities.FamilyHomeActivity;
import com.example.helmethero.activities.RiderHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken");
            ref.setValue(token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "HelmetHero";
        String message = "You have a new notification!";
        String googleMapsUrl = null;
        String userRole = "Family Member"; // Default

        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title"))
                title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body"))
                message = remoteMessage.getData().get("body");
            if (remoteMessage.getData().containsKey("googleMapsUrl"))
                googleMapsUrl = remoteMessage.getData().get("googleMapsUrl");
            if (remoteMessage.getData().containsKey("userRole"))
                userRole = remoteMessage.getData().get("userRole");
        }

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null)
                title = remoteMessage.getNotification().getTitle();
            if (remoteMessage.getNotification().getBody() != null)
                message = remoteMessage.getNotification().getBody();
        }

        showNotification(title, message, googleMapsUrl, userRole);
    }

    private void showNotification(String title, String message, String googleMapsUrl, String userRole) {
        Intent intent;

        if (googleMapsUrl != null && !googleMapsUrl.isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsUrl));
        } else {
            // Navigate to home screen based on user role
            if ("Rider".equalsIgnoreCase(userRole)) {
                intent = new Intent(this, RiderHomeActivity.class);
            } else {
                intent = new Intent(this, FamilyHomeActivity.class);
            }
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

        // For Android O and above
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