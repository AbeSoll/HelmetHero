package com.example.helmethero.adapters;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.example.helmethero.models.Alert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private final List<Alert> alertList;

    public AlertsAdapter(List<Alert> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alertList.get(position);

        // Rider name & profile image
        holder.textRiderName.setText(alert.getRiderName() != null ? alert.getRiderName() : "Unknown Rider");
        String imageUrl = alert.getProfileImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // Alert type badge
        holder.textAlertType.setText(alert.getType() != null ? alert.getType().toUpperCase() : "-");
        if ("IMPACT".equalsIgnoreCase(alert.getType()) || "SOS".equalsIgnoreCase(alert.getType())) {
            holder.textAlertType.setBackgroundResource(R.drawable.bg_alert_type_badge);
        } else {
            holder.textAlertType.setBackgroundResource(R.drawable.bg_status_badge);
        }

        holder.textAlertTime.setText("Time: " + (alert.getTime() != null ? alert.getTime() : "-"));
        holder.textAlertLocation.setText("Location: " + (alert.getLocation() != null ? alert.getLocation() : "-"));

        // Status badge logic
        String status = alert.getStatus() != null ? alert.getStatus().toUpperCase() : "-";
        holder.textAlertStatus.setText(status);

        if ("NEW".equalsIgnoreCase(status)) {
            holder.textAlertStatus.setBackgroundResource(R.drawable.bg_status_badge); // Green
            holder.textAlertStatus.setOnClickListener(v -> {
                String familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String alertId = alert.getAlertId();
                DatabaseReference alertRef = FirebaseDatabase.getInstance()
                        .getReference("Alerts")
                        .child(familyUid)
                        .child(alertId);

                alertRef.child("status").setValue("SEEN");
                alertRef.child("seen").setValue(true);

                alert.setStatus("SEEN");
                notifyItemChanged(holder.getAdapterPosition());

                // Optional: Disable further clicks
                holder.textAlertStatus.setOnClickListener(null);

                Toast.makeText(holder.itemView.getContext(), "Alert seen.", Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.textAlertStatus.setBackgroundColor(Color.parseColor("#AAB2B5")); // Grey for seen
            holder.textAlertStatus.setOnClickListener(null); // Disable click
        }

        // === FEATURE: Klik satu alert card navigate ke lokasi Google Maps ===
        holder.itemView.setOnClickListener(v -> {
            String locationStr = alert.getLocation();
            if (locationStr != null && locationStr.contains(",")) {
                String[] locParts = locationStr.split(",");
                try {
                    double lat = Double.parseDouble(locParts[0]);
                    double lng = Double.parseDouble(locParts[1]);
                    String label = alert.getRiderName() + " Impact Location";
                    String uri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + label + ")";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(v.getContext().getPackageManager()) != null) {
                        v.getContext().startActivity(intent);
                    } else {
                        // fallback jika tiada Google Maps
                        String webUri = "https://maps.google.com/?q=" + lat + "," + lng;
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                        v.getContext().startActivity(webIntent);
                    }
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Invalid coordinates.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(v.getContext(), "Location unavailable.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProfile;
        TextView textRiderName, textAlertType, textAlertTime, textAlertLocation, textAlertStatus;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            textRiderName = itemView.findViewById(R.id.textRiderName);
            textAlertType = itemView.findViewById(R.id.textAlertType);
            textAlertTime = itemView.findViewById(R.id.textAlertTime);
            textAlertLocation = itemView.findViewById(R.id.textAlertLocation);
            textAlertStatus = itemView.findViewById(R.id.textAlertStatus);
        }
    }
}