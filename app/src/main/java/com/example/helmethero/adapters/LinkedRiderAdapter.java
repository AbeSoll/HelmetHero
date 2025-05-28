package com.example.helmethero.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // <-- Add this
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // <-- Add this
import com.example.helmethero.R;
import com.example.helmethero.models.User;

import java.util.List;

public class LinkedRiderAdapter extends RecyclerView.Adapter<LinkedRiderAdapter.ViewHolder> {
    private final List<User> riderList;
    private final OnRiderClickListener listener;

    // Click listener interface
    public interface OnRiderClickListener {
        void onRiderClicked(User rider);
    }

    public LinkedRiderAdapter(List<User> list, OnRiderClickListener listener) {
        this.riderList = list;
        this.listener = listener;
    }

    public LinkedRiderAdapter(List<User> list) {
        this(list, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_linked_rider, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User rider = riderList.get(position);
        holder.name.setText(rider.getName());
        holder.email.setText("Email: " + (rider.getEmail() == null ? "-" : rider.getEmail()));
        holder.phone.setText("Phone: " + (rider.getPhone() == null ? "-" : rider.getPhone()));

        // NEW: Set profile image (if available)
        if (rider.getProfileImageUrl() != null && !rider.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(rider.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // Set click listener if needed
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onRiderClicked(rider));
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return riderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile; // NEW
        TextView name, email, phone;

        ViewHolder(View v) {
            super(v);
            imgProfile = v.findViewById(R.id.imgProfile); // NEW
            name = v.findViewById(R.id.textRiderName);
            email = v.findViewById(R.id.textRiderEmail);
            phone = v.findViewById(R.id.textRiderPhone);
        }
    }
}