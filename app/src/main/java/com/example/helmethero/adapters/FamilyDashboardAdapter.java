package com.example.helmethero.adapters; // Use your correct package!

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.example.helmethero.models.RiderDashboardData;

import java.util.List;

public class FamilyDashboardAdapter extends RecyclerView.Adapter<FamilyDashboardAdapter.RiderViewHolder> {

    private List<RiderDashboardData> riderList;
    private Context context;

    // Listener for button clicks (optional, for navigation)
    public interface OnRiderCardActionListener {
        void onViewLive(RiderDashboardData rider);
        void onViewHistory(RiderDashboardData rider);
        void onViewAlerts(RiderDashboardData rider);
    }
    private OnRiderCardActionListener actionListener;
    public void setOnRiderCardActionListener(OnRiderCardActionListener listener) {
        this.actionListener = listener;
    }

    public FamilyDashboardAdapter(List<RiderDashboardData> riderList, Context context) {
        this.riderList = riderList;
        this.context = context;
    }

    @NonNull
    @Override
    public RiderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_dashboard_rider_card, parent, false);
        return new RiderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RiderViewHolder holder, int position) {
        RiderDashboardData rider = riderList.get(position);

        holder.textRiderName.setText(rider.name != null ? rider.name : "Rider");
        holder.textLastTrip.setText(rider.lastTripSummary != null ? rider.lastTripSummary : "-");

        // Status Badge
        if (rider.activeRide) {
            holder.textStatusBadge.setText("ACTIVE RIDE");
            holder.textStatusBadge.setBackgroundResource(R.drawable.bg_status_active); // create green badge bg
        } else {
            holder.textStatusBadge.setText("NOT RIDING");
            holder.textStatusBadge.setBackgroundResource(R.drawable.bg_status_inactive); // create gray badge bg
        }

        // Profile
        if (rider.profileImageUrl != null && !rider.profileImageUrl.isEmpty()) {
            Glide.with(context).load(rider.profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // Button click logic
        holder.btnViewLive.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewLive(rider);
        });
        holder.btnViewHistory.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewHistory(rider);
        });
        holder.btnViewAlerts.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewAlerts(rider);
        });
    }

    @Override
    public int getItemCount() {
        return riderList.size();
    }

    public static class RiderViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView textRiderName, textStatusBadge, textLastTrip;
        Button btnViewLive, btnViewHistory, btnViewAlerts;
        public RiderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            textRiderName = itemView.findViewById(R.id.textRiderName);
            textStatusBadge = itemView.findViewById(R.id.textStatusBadge);
            textLastTrip = itemView.findViewById(R.id.textLastTrip);
            btnViewLive = itemView.findViewById(R.id.btnViewLive);
            btnViewHistory = itemView.findViewById(R.id.btnViewHistory);
            btnViewAlerts = itemView.findViewById(R.id.btnViewAlerts);
        }
    }
}
