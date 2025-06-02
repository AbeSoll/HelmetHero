package com.example.helmethero.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.example.helmethero.models.User;

import java.util.List;
import java.util.Set;

public class LinkedRiderAdapter extends RecyclerView.Adapter<LinkedRiderAdapter.ViewHolder> {

    private final List<User> riderList;
    private final Set<String> selectedRiderUids;
    private final SelectionChangeListener selectionChangeListener;
    private OnRiderClickListener riderClickListener;

    public interface SelectionChangeListener {
        void onSelectionChanged();
    }

    public interface OnRiderClickListener {
        void onRiderClicked(User rider);
    }

    public LinkedRiderAdapter(List<User> riderList,
                              Set<String> selectedRiderUids,
                              SelectionChangeListener selectionChangeListener) {
        this.riderList = riderList;
        this.selectedRiderUids = selectedRiderUids;
        this.selectionChangeListener = selectionChangeListener;
    }

    // ✅ Setter to be called externally
    public void setOnRiderItemClickListener(OnRiderClickListener listener) {
        this.riderClickListener = listener;
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
        holder.phone.setText("Phone: " + (rider.getPhone() == null ? "-" : rider.getPhone()));

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

        boolean inSelectionMode = !selectedRiderUids.isEmpty();
        boolean isSelected = selectedRiderUids.contains(rider.getUid());

        // Show checkbox only in selection mode, otherwise hide all
        holder.checkBox.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected);

        holder.itemView.setOnLongClickListener(v -> {
            if (!inSelectionMode) {
                // Start selection mode
                selectedRiderUids.add(rider.getUid());
                notifyDataSetChanged();
                if (selectionChangeListener != null) selectionChangeListener.onSelectionChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (!selectedRiderUids.isEmpty()) {
                // Toggle selection for this item
                if (isSelected) {
                    selectedRiderUids.remove(rider.getUid());
                } else {
                    selectedRiderUids.add(rider.getUid());
                }
                notifyDataSetChanged();
                if (selectionChangeListener != null) selectionChangeListener.onSelectionChanged();
            } else if (riderClickListener != null) {
                riderClickListener.onRiderClicked(rider);
            }
        });

        // Prevent user from directly interacting with the checkbox (only through item click)
        holder.checkBox.setOnClickListener(v -> {
            // do nothing (optional: to disable direct checkbox click)
        });
    }

    @Override
    public int getItemCount() {
        return riderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView name, phone;
        CheckBox checkBox;

        ViewHolder(View v) {
            super(v);
            imgProfile = v.findViewById(R.id.imgProfile);
            name = v.findViewById(R.id.textRiderName);
            phone = v.findViewById(R.id.textRiderPhone);
            checkBox = v.findViewById(R.id.checkboxSelect);
        }
    }
}