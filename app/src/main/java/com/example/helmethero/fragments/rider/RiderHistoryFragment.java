package com.example.helmethero.fragments.rider;

import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.helmethero.R;
import com.example.helmethero.adapters.DateAdapter;
import com.example.helmethero.adapters.TripHistoryAdapter;
import com.example.helmethero.models.Trip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class RiderHistoryFragment extends Fragment {

    private DateAdapter dateAdapter;
    private TripHistoryAdapter tripHistoryAdapter;
    private List<Trip> tripList = new ArrayList<>();
    private List<Integer> dayList = new ArrayList<>();
    private RecyclerView recyclerDatePicker, recyclerTripHistory;
    private TextView textMonthPicker;
    private LinearLayout layoutEmptyState, layoutMonthPicker;
    private ImageView btnMonthPrev, btnMonthNext;
    private DatabaseReference tripsRef;

    // Restore state
    private int selectedYear, selectedMonth, selectedDay;
    private int presentDay = -1; // track present day of month for highlight

    private static final String STATE_YEAR = "state_year";
    private static final String STATE_MONTH = "state_month";
    private static final String STATE_DAY = "state_day";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_trip_history, container, false);

        recyclerDatePicker = view.findViewById(R.id.recyclerDatePicker);
        recyclerTripHistory = view.findViewById(R.id.recyclerTripHistory);

        // --- Month picker views (NEW) ---
        layoutMonthPicker = view.findViewById(R.id.layoutMonthPicker);
        btnMonthPrev = view.findViewById(R.id.btnMonthPrev);
        btnMonthNext = view.findViewById(R.id.btnMonthNext);
        textMonthPicker = view.findViewById(R.id.textMonthPicker);

        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        recyclerDatePicker.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerTripHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        tripHistoryAdapter = new TripHistoryAdapter(tripList, trip -> {
            Fragment detailFragment = RiderTripDetailFragment.newInstance(trip);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerTripHistory.setAdapter(tripHistoryAdapter);

        // Restore from bundle if available
        if (savedInstanceState != null) {
            selectedYear = savedInstanceState.getInt(STATE_YEAR, Calendar.getInstance().get(Calendar.YEAR));
            selectedMonth = savedInstanceState.getInt(STATE_MONTH, Calendar.getInstance().get(Calendar.MONTH));
            selectedDay = savedInstanceState.getInt(STATE_DAY, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        } else {
            Calendar calendar = Calendar.getInstance();
            selectedYear = calendar.get(Calendar.YEAR);
            selectedMonth = calendar.get(Calendar.MONTH);
            selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        updatePresentDay();
        updateMonthPickerText();
        setupDatePicker();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tripsRef = FirebaseDatabase.getInstance().getReference("Trips").child(uid);

        // ========== Chevron Button Logic ==========
        btnMonthPrev.setOnClickListener(v -> {
            selectedMonth--;
            if (selectedMonth < 0) {
                selectedMonth = 11;
                selectedYear--;
            }
            selectedDay = 1;
            updatePresentDay();
            updateMonthPickerText();
            updateDatePicker();
            loadTripsForSelectedDate();
        });
        btnMonthNext.setOnClickListener(v -> {
            selectedMonth++;
            if (selectedMonth > 11) {
                selectedMonth = 0;
                selectedYear++;
            }
            selectedDay = 1;
            updatePresentDay();
            updateMonthPickerText();
            updateDatePicker();
            loadTripsForSelectedDate();
        });
        layoutMonthPicker.setOnClickListener(v -> {
            // Optional: Boleh buka MaterialDatePicker asal
            MaterialDatePicker<Long> datePicker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select Month")
                            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                            .build();

            datePicker.show(getParentFragmentManager(), "MONTH_PICKER");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(selection);
                selectedYear = calendar.get(Calendar.YEAR);
                selectedMonth = calendar.get(Calendar.MONTH);

                // If selected month is this month, highlight today, else default to day 1
                Calendar now = Calendar.getInstance();
                if (now.get(Calendar.YEAR) == selectedYear && now.get(Calendar.MONTH) == selectedMonth) {
                    selectedDay = now.get(Calendar.DAY_OF_MONTH);
                } else {
                    selectedDay = 1;
                }
                updatePresentDay();
                updateMonthPickerText();
                updateDatePicker();
                loadTripsForSelectedDate();
            });
        });
        // ========== END Chevron Button Logic ==========

        loadTripsForSelectedDate();

        return view;
    }

    // Save state
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_YEAR, selectedYear);
        outState.putInt(STATE_MONTH, selectedMonth);
        outState.putInt(STATE_DAY, selectedDay);
    }

    private void updatePresentDay() {
        // Calculate presentDay for current selected year/month
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) == selectedYear && now.get(Calendar.MONTH) == selectedMonth) {
            presentDay = now.get(Calendar.DAY_OF_MONTH);
        } else {
            presentDay = -1;
        }
    }

    private void updateMonthPickerText() {
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(new GregorianCalendar(selectedYear, selectedMonth, 1).getTime());
        textMonthPicker.setText(monthYear.toUpperCase());
    }

    private void setupDatePicker() {
        buildDayListAndAdapter();
    }

    private void buildDayListAndAdapter() {
        dayList.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= maxDay; i++) dayList.add(i);

        int highlightDay = selectedDay; // always use the restored value
        if (dateAdapter == null) {
            dateAdapter = new DateAdapter(dayList, selectedMonth, selectedYear, (day, month, year) -> {
                selectedDay = day;
                selectedMonth = month;
                selectedYear = year;
                updatePresentDay(); // Just in case user navigates (optional)
                loadTripsForSelectedDate();
                buildDayListAndAdapter(); // To refresh highlight
            });
            recyclerDatePicker.setAdapter(dateAdapter);
        }
        dateAdapter.updateDays(dayList, selectedMonth, selectedYear, highlightDay, presentDay);

        // Auto-scroll to currently selected day
        recyclerDatePicker.post(() -> recyclerDatePicker.scrollToPosition(highlightDay - 1));
    }

    private void updateDatePicker() {
        buildDayListAndAdapter();
    }

    private void loadTripsForSelectedDate() {
        String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedYear, selectedMonth + 1, selectedDay);

        tripsRef.orderByChild("date").equalTo(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tripList.clear();
                        for (DataSnapshot tripSnap : snapshot.getChildren()) {
                            Trip trip = tripSnap.getValue(Trip.class);
                            if (trip != null) tripList.add(trip);
                        }
                        // SORT DESCENDING: latest first
                        Collections.sort(tripList, (t1, t2) -> {
                            // Assuming you store timestamp as yyyy-MM-dd HH:mm:ss, or use long/int
                            return t2.getTimestamp().compareTo(t1.getTimestamp());
                        });
                        tripHistoryAdapter.notifyDataSetChanged();

                        layoutEmptyState.setVisibility(tripList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
}
