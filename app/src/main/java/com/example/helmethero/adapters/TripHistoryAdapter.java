package com.example.helmethero.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;
import com.example.helmethero.fragments.rider.RiderTripDetailFragment;
import com.example.helmethero.models.Trip;

import java.util.List;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {

    private final List<Trip> tripList;

    public TripHistoryAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_history, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        holder.dateText.setText(trip.getTimestamp());
        holder.durationText.setText("Duration • " + trip.getDuration());
        holder.distanceText.setText("Distance • " + trip.getDistance());

        // ✅ Use plain string concatenation to avoid crash
        holder.avgSpeedText.setText("Avg Speed • " + trip.getAvgSpeed());

        holder.statusText.setText(trip.getStatus());

        holder.itemView.setOnClickListener(v -> {
            RiderTripDetailFragment detailFragment = new RiderTripDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable("trip", trip);
            detailFragment.setArguments(args);

            ((FragmentActivity) v.getContext()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
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
}
