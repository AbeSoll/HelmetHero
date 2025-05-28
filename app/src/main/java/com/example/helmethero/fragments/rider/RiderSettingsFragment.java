package com.example.helmethero.fragments.rider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.helmethero.R;
import com.example.helmethero.activities.LoginActivity;

import static android.content.Context.MODE_PRIVATE;

public class RiderSettingsFragment extends Fragment {

    private LinearLayout btnAccountInfo, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_settings, container, false);

        btnAccountInfo = view.findViewById(R.id.btnAccountInfo);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnAccountInfo.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, new RiderProfileSetupFragment());
            ft.addToBackStack(null);
            ft.commit();
        });

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        SharedPreferences preferences = requireActivity().getSharedPreferences("HelmetHeroPrefs", MODE_PRIVATE);
                        preferences.edit().clear().apply();

                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }
}
