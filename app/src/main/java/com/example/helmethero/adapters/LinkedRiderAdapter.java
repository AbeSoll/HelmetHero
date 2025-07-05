package com.example.helmethero.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User rider = riderList.get(position);

        holder.name.setText(rider.getName());

        // ✅ UPDATED: Handle phone number and call button visibility
        if (rider.getPhone() != null && !rider.getPhone().trim().isEmpty()) {
            holder.phone.setText("Phone: " + rider.getPhone());
            holder.btnCallRider.setVisibility(View.VISIBLE);
        } else {
            holder.phone.setText("Phone: Not available");
            holder.btnCallRider.setVisibility(View.GONE);
        }

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

        // Show checkbox and hide call button when in selection mode
        if (inSelectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.btnCallRider.setVisibility(View.GONE); // Hide call button during selection
        } else {
            holder.checkBox.setVisibility(View.GONE);
            // Re-evaluate call button visibility based on phone number
            if (rider.getPhone() != null && !rider.getPhone().trim().isEmpty()) {
                holder.btnCallRider.setVisibility(View.VISIBLE);
            } else {
                holder.btnCallRider.setVisibility(View.GONE);
            }
        }
        holder.checkBox.setChecked(isSelected);

        // ✅ NEW: Set OnClickListener for the call button
        holder.btnCallRider.setOnClickListener(v -> {
            String phoneNumber = rider.getPhone();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + phoneNumber));

                Context context = holder.itemView.getContext();
                if (dialIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(dialIntent);
                } else {
                    Toast.makeText(context, "No application can handle this action.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!inSelectionMode) {
                selectedRiderUids.add(rider.getUid());
                notifyDataSetChanged();
                if (selectionChangeListener != null) selectionChangeListener.onSelectionChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (!selectedRiderUids.isEmpty()) {
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
    }

    @Override
    public int getItemCount() {
        return riderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView name, phone;
        CheckBox checkBox;
        ImageButton btnCallRider; // ✅ NEW

        ViewHolder(View v) {
            super(v);
            imgProfile = v.findViewById(R.id.imgProfile);
            name = v.findViewById(R.id.textRiderName);
            phone = v.findViewById(R.id.textRiderPhone);
            checkBox = v.findViewById(R.id.checkboxSelect);
            btnCallRider = v.findViewById(R.id.btnCallRider); // ✅ NEW
        }
    }
}
