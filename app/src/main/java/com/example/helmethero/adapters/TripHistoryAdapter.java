package com.example.helmethero.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.models.Trip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    private final List<Trip> tripList;
    private final OnTripClickListener clickListener;

    public TripHistoryAdapter(List<Trip> tripList, OnTripClickListener clickListener) {
        this.tripList = tripList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_history, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        String formattedDate = formatDate(trip.getTimestamp());
        String formattedTime = formatTime(trip.getTimestamp());

        holder.dateText.setText("" + formattedDate + "   |   " + formattedTime);
        holder.durationText.setText("Duration: " + trip.getDuration());
        holder.distanceText.setText("Distance: " + trip.getDistance());
        holder.avgSpeedText.setText("Avg Speed: " + trip.getAvgSpeed());
        holder.statusText.setText(trip.getStatus() != null ? trip.getStatus() : "-");

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTripClick(trip);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, durationText, distanceText, avgSpeedText, statusText;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.textTripDate);
            durationText = itemView.findViewById(R.id.textTripDuration);
            distanceText = itemView.findViewById(R.id.textTripDistance);
            avgSpeedText = itemView.findViewById(R.id.textTripAvgSpeed);
            statusText = itemView.findViewById(R.id.textTripStatus);
        }
    }

    // Formatter for "1 Jan 2025"
    private String formatDate(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            Date date = input.parse(timestamp);
            return (date != null) ? output.format(date) : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    // Formatter for "3:45 PM"
    private String formatTime(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = input.parse(timestamp);
            return (date != null) ? output.format(date) : "-";
        } catch (Exception e) {
            return "-";
        }
    }
}