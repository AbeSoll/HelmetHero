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

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder> {

    private final List<User> contactList;
    private final Set<String> selectedUids;
    private final OnSelectionChangedListener selectionChangedListener;
    private OnItemClickListener itemClickListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    public interface OnItemClickListener {
        void onContactClick(User user);
    }

    public EmergencyContactAdapter(List<User> contactList, Set<String> selectedUids, OnSelectionChangedListener listener) {
        this.contactList = contactList;
        this.selectedUids = selectedUids;
        this.selectionChangedListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_contact, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = contactList.get(position);

        holder.name.setText(user.getName());
        holder.phone.setText(user.getPhone() == null ? "-" : user.getPhone());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.ic_profile);
        }

        boolean inSelectionMode = !selectedUids.isEmpty();
        boolean isSelected = selectedUids.contains(user.getUid());

        // Only show checkbox in selection mode
        holder.checkBox.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected);

        // Prevent ghost checkbox: force view refresh for all on mode change
        holder.itemView.setOnLongClickListener(v -> {
            if (!inSelectionMode) {
                selectedUids.add(user.getUid());
                notifyDataSetChanged();
                if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (!selectedUids.isEmpty()) {
                // Toggle this selection
                if (isSelected) selectedUids.remove(user.getUid());
                else selectedUids.add(user.getUid());

                // If nothing left, exit selection mode (force all views to refresh/hide checkbox)
                notifyDataSetChanged();
                if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged();
            } else if (itemClickListener != null) {
                itemClickListener.onContactClick(user);
            }
        });

        // Optional: disable checkbox direct interaction, only via item click/long press
        holder.checkBox.setOnClickListener(v -> {
            // do nothing
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name, phone;
        CheckBox checkBox;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatarImage);
            name = v.findViewById(R.id.contactName);
            phone = v.findViewById(R.id.contactPhone);
            checkBox = v.findViewById(R.id.checkboxSelect);
        }
    }
}