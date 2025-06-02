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

import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private final List<Alert> alertList;
    private final AlertSeenListener listener;

    public interface AlertSeenListener {
        void onAlertSeen(String alertId);
    }

    public AlertsAdapter(List<Alert> alertList, AlertSeenListener listener) {
        this.alertList = alertList;
        this.listener = listener;
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

        holder.textAlertType.setText(alert.getType() != null ? alert.getType().toUpperCase() : "-");
        if ("IMPACT".equalsIgnoreCase(alert.getType()) || "SOS".equalsIgnoreCase(alert.getType())) {
            holder.textAlertType.setBackgroundResource(R.drawable.bg_alert_type_badge);
        } else {
            holder.textAlertType.setBackgroundResource(R.drawable.bg_status_badge);
        }

        holder.textAlertTime.setText("Time: " + (alert.getFormattedTime() != null ? alert.getFormattedTime() : "-"));
        holder.textAlertLocation.setText("Location: " + (alert.getLocation() != null ? alert.getLocation() : "-"));

        String status = alert.getStatus() != null ? alert.getStatus().toUpperCase() : "-";
        holder.textAlertStatus.setText(status);

        if ("NEW".equalsIgnoreCase(status)) {
            holder.textAlertStatus.setBackgroundResource(R.drawable.bg_status_badge);
            holder.textAlertStatus.setTextColor(Color.WHITE);
            holder.textAlertStatus.setClickable(true);
            holder.textAlertStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertSeen(alert.getId());
                }
                alert.setStatus("SEEN");
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos);
                }
                Toast.makeText(holder.itemView.getContext(), "Alert marked as seen.", Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.textAlertStatus.setBackgroundColor(Color.parseColor("#AAB2B5"));
            holder.textAlertStatus.setTextColor(Color.WHITE);
            holder.textAlertStatus.setOnClickListener(null);
            holder.textAlertStatus.setClickable(false);
        }

        // **UPDATE HERE: Tap on itemView also marks as SEEN if status is NEW**
        holder.itemView.setOnClickListener(v -> {
            // 1. Mark as seen if still NEW
            if ("NEW".equalsIgnoreCase(status) && listener != null) {
                listener.onAlertSeen(alert.getId());
                alert.setStatus("SEEN");
                notifyItemChanged(holder.getAdapterPosition());
            }
            // 2. (Original) Open Google Maps
            String locationStr = alert.getLocation();
            if (locationStr != null && locationStr.contains(",")) {
                String[] locParts = locationStr.split(",");
                try {
                    double lat = Double.parseDouble(locParts[0]);
                    double lng = Double.parseDouble(locParts[1]);
                    String label = (alert.getRiderName() == null ? "" : alert.getRiderName()) + " Impact Location";
                    String uri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + label + ")";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(v.getContext().getPackageManager()) != null) {
                        v.getContext().startActivity(intent);
                    } else {
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