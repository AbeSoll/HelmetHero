package com.example.helmethero.fragments.family;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.helmethero.R;
import com.example.helmethero.adapters.DateAdapter;
import com.example.helmethero.adapters.TripHistoryAdapter;
import com.example.helmethero.models.Trip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class FamilyTripListFragment extends Fragment {

    private String riderUid, riderName;

    private RecyclerView recyclerDatePicker, recyclerFamilyTrips;
    private DateAdapter dateAdapter;
    private TripHistoryAdapter tripHistoryAdapter;
    private final List<Trip> tripList = new ArrayList<>();
    private final List<Integer> dayList = new ArrayList<>();
    private TextView textRiderTitle, textMonthPicker;
    private LinearLayout layoutEmptyState, layoutMonthPicker;
    private ImageView buttonBack, btnMonthPrev, btnMonthNext;

    private DatabaseReference tripsRef;

    private int selectedYear, selectedMonth, selectedDay;
    private int presentDay = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            riderUid = getArguments().getString("riderUid");
            riderName = getArguments().getString("riderName");
        }
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_trip_list, container, false);

        recyclerDatePicker = view.findViewById(R.id.recyclerDatePicker);
        recyclerFamilyTrips = view.findViewById(R.id.recyclerFamilyTrips);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        textRiderTitle = view.findViewById(R.id.textRiderTitle);

        // --- Bulan picker views
        layoutMonthPicker = view.findViewById(R.id.layoutMonthPicker);
        btnMonthPrev = view.findViewById(R.id.btnMonthPrev);
        btnMonthNext = view.findViewById(R.id.btnMonthNext);
        textMonthPicker = view.findViewById(R.id.textMonthPicker);

        // ======= BUTTON BACK LOGIC START HERE =======
        buttonBack = view.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.family_fragment_container, new FamilyLinkedRidersFragment())
                    .commit();
        });
        // ======= BUTTON BACK LOGIC END =======

        // --- MONTH PICKER CHEVRON LOGIC
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
            // Optional: Boleh kekalkan open MaterialDatePicker asal
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

        recyclerDatePicker.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerFamilyTrips.setLayoutManager(new LinearLayoutManager(getContext()));

        tripHistoryAdapter = new TripHistoryAdapter(tripList, trip -> {
            Fragment detailFragment = FamilyTripDetailFragment.newInstance(trip, riderUid);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.family_fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerFamilyTrips.setAdapter(tripHistoryAdapter);

        if (riderName != null && !riderName.isEmpty()) {
            textRiderTitle.setText("Trip History: " + riderName);
        }

        if (riderUid == null || riderUid.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerFamilyTrips.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No rider selected. Please try again.", Toast.LENGTH_SHORT).show();
            return view;
        }

        tripsRef = FirebaseDatabase.getInstance().getReference("Trips").child(riderUid);

        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);

        updatePresentDay();
        updateMonthPickerText();
        setupDatePicker();
        loadTripsForSelectedDate();

        return view;
    }

    private void updatePresentDay() {
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

        int highlightDay = selectedDay;
        if (dateAdapter == null) {
            dateAdapter = new DateAdapter(dayList, selectedMonth, selectedYear, (day, month, year) -> {
                selectedDay = day;
                selectedMonth = month;
                selectedYear = year;
                updatePresentDay();
                loadTripsForSelectedDate();
                buildDayListAndAdapter();
            });
            recyclerDatePicker.setAdapter(dateAdapter);
        }

        dateAdapter.updateDays(dayList, selectedMonth, selectedYear, highlightDay, presentDay);
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

                        tripList.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));
                        tripHistoryAdapter.notifyDataSetChanged();
                        layoutEmptyState.setVisibility(tripList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public static FamilyTripListFragment newInstance(String riderUid, String riderName) {
        FamilyTripListFragment fragment = new FamilyTripListFragment();
        Bundle args = new Bundle();
        args.putString("riderUid", riderUid);
        args.putString("riderName", riderName);
        fragment.setArguments(args);
        return fragment;
    }
}