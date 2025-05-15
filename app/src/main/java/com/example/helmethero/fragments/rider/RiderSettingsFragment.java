package com.example.helmethero.fragments.rider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.helmethero.R;
import com.example.helmethero.activities.LoginActivity;

import static android.content.Context.MODE_PRIVATE;

public class RiderSettingsFragment extends Fragment {

    private LinearLayout btnLanguage, btnAccountInfo, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_settings, container, false);

        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnAccountInfo = view.findViewById(R.id.btnAccountInfo);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Language Button (Future Implementation)
        btnLanguage.setOnClickListener(v -> {
            // TODO: Implement language selection
        });

        // Navigate to RiderProfileSetupFragment for account editing
        btnAccountInfo.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, new RiderProfileSetupFragment());
            ft.addToBackStack(null);
            ft.commit();
        });

        // ✅ Logout Button Logic
        btnLogout.setOnClickListener(v -> {
            // Clear saved login session
            SharedPreferences preferences = requireActivity().getSharedPreferences("HelmetHeroPrefs", MODE_PRIVATE);
            preferences.edit().clear().apply();

            // Go to LoginActivity and clear back stack
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}