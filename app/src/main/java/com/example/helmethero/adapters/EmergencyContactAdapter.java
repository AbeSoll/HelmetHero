package com.example.helmethero.adapters;

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

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    private final List<User> contacts;
    private final Set<String> selectedIds;

    public EmergencyContactAdapter(List<User> contacts, Set<String> selectedIds) {
        this.contacts = contacts;
        this.selectedIds = selectedIds;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = contacts.get(position);
        holder.nameText.setText(user.getName());
        holder.emailText.setText(user.getEmail() != null ? user.getEmail() : "Email not available");
        holder.phoneText.setText(user.getPhone() != null ? user.getPhone() : "Phone not available");

        // Load profile image using Glide
        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(holder.avatarImage);

        holder.checkBox.setOnCheckedChangeListener(null); // prevent callback reuse issues
        holder.checkBox.setChecked(selectedIds.contains(user.getUid()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(user.getUid());
            } else {
                selectedIds.remove(user.getUid());
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText, phoneText;
        CheckBox checkBox;
        ImageView avatarImage;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.contactName);
            emailText = itemView.findViewById(R.id.contactEmail);
            phoneText = itemView.findViewById(R.id.contactPhone);
            checkBox = itemView.findViewById(R.id.contactCheckbox);
            avatarImage = itemView.findViewById(R.id.avatarImage);
        }
    }
}
