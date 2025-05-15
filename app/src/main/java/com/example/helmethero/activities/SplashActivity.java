package com.example.helmethero.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay 2.5 seconds before checking login session
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                // ✅ Check stored role from SharedPreferences
                SharedPreferences prefs = getSharedPreferences("HelmetHeroPrefs", MODE_PRIVATE);
                String role = prefs.getString("role", "");

                if (role.equals("Rider")) {
                    startActivity(new Intent(SplashActivity.this, RiderHomeActivity.class));
                } else if (role.equals("Family Member")) {
                    startActivity(new Intent(SplashActivity.this, FamilyHomeActivity.class));
                } else {
                    // No valid role stored – fallback to login
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

            } else {
                // Not logged in
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }

            finish(); // Close SplashActivity so user can't go back

        }, 2500);
    }
}
