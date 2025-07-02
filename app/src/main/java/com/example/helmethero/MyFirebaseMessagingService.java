package com.example.helmethero;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
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

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String SOS_CHANNEL_ID = "sos_emergency_channel";
    private static final String NORMAL_CHANNEL_ID = "helmethero_channel";

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
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        createNotificationChannels();

        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) return;

        String title = data.getOrDefault("title", "HelmetHero");
        String message = data.getOrDefault("body", "You have a new notification!");
        String googleMapsUrl = data.get("googleMapsUrl");
        String userRole = data.getOrDefault("userRole", "Family Member");
        String sosType = data.get("sosType");

        showNotification(title, message, googleMapsUrl, userRole, sosType);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // SOS Emergency Channel
            Uri sosUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/emergency_sos");
            AudioAttributes sosAudioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build();

            NotificationChannel sosChannel = new NotificationChannel(
                    SOS_CHANNEL_ID,
                    "🚨 SOS Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            sosChannel.setSound(sosUri, sosAudioAttributes);
            sosChannel.setDescription("Critical emergency alerts with custom sound");
            sosChannel.enableVibration(true);
            sosChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            sosChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            sosChannel.setBypassDnd(true);
            notificationManager.createNotificationChannel(sosChannel);

            // Normal Notification Channel
            Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationChannel normalChannel = new NotificationChannel(
                    NORMAL_CHANNEL_ID,
                    "HelmetHero Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            normalChannel.setSound(defaultUri, null);
            normalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(normalChannel);
        }
    }

    private void showNotification(String title, String message, String googleMapsUrl, String userRole, String sosType) {
        Intent intent;

        if (googleMapsUrl != null && !googleMapsUrl.isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsUrl));
        } else {
            intent = "Rider".equalsIgnoreCase(userRole)
                    ? new Intent(this, RiderHomeActivity.class)
                    : new Intent(this, FamilyHomeActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "SOS".equalsIgnoreCase(sosType) ? SOS_CHANNEL_ID : NORMAL_CHANNEL_ID;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_helmethero_logo)
                .setContentTitle("SOS".equalsIgnoreCase(sosType) ? "🚨 " + title : title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority("SOS".equalsIgnoreCase(sosType)
                        ? NotificationCompat.PRIORITY_MAX
                        : NotificationCompat.PRIORITY_HIGH);

        if ("SOS".equalsIgnoreCase(sosType)) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    .setFullScreenIntent(pendingIntent, true);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
