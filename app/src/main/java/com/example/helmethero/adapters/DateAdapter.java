package com.example.helmethero.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helmethero.R;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DayViewHolder> {

    private List<Integer> days;
    private int selectedPosition = 0;
    private int presentDay = -1; // 0-based, -1 if not this month
    private int month, year;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(int day, int month, int year);
    }

    // ** NEW: Add presentDay param **
    public DateAdapter(List<Integer> days, int month, int year, OnDayClickListener listener) {
        this.days = days;
        this.month = month;
        this.year = year;
        this.listener = listener;
    }

    // ** NEW: Add presentDay param **
    @SuppressLint("NotifyDataSetChanged")
    public void updateDays(List<Integer> newDays, int month, int year, int highlightDay, int presentDay) {
        this.days = newDays;
        this.month = month;
        this.year = year;
        if (highlightDay >= 1 && highlightDay <= days.size()) {
            this.selectedPosition = highlightDay - 1; // 0-based index
        } else {
            this.selectedPosition = 0;
        }
        if (presentDay >= 1 && presentDay <= days.size()) {
            this.presentDay = presentDay - 1; // 0-based index
        } else {
            this.presentDay = -1; // not visible
        }
        notifyDataSetChanged();
    }

    public int getSelectedDay() {
        return days.get(selectedPosition);
    }
    public int getSelectedMonth() {
        return month;
    }
    public int getSelectedYear() {
        return year;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = days.get(position);

        boolean isSelected = position == selectedPosition;
        boolean isPresentDay = (position == presentDay);

        // PRIORITY: Selected > Present
        if (isSelected) {
            holder.textDay.setBackgroundResource(R.drawable.bg_date_selected); // blue
            holder.textDay.setTextColor(Color.WHITE);
        } else if (isPresentDay) {
            holder.textDay.setBackgroundResource(R.drawable.bg_date_present); // grey
            holder.textDay.setTextColor(Color.BLACK);
        } else {
            holder.textDay.setBackgroundResource(R.drawable.bg_date_unselected); // white
            holder.textDay.setTextColor(Color.BLACK);
        }

        holder.textDay.setText(String.valueOf(day));
        holder.textDay.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onDayClick(day, month, year);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView textDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textDay);
        }
    }
}