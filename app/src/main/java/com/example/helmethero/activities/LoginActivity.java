package com.example.helmethero.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Spinner roleSpinner;
    Button loginButton;
    TextView signupText, forgotPassword;
    String selectedRole = "";
    FirebaseAuth auth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        loginButton = findViewById(R.id.loginButton);
        signupText = findViewById(R.id.signupText);
        forgotPassword = findViewById(R.id.forgotPassword);

        auth = FirebaseAuth.getInstance();

        String[] roles = {"Select Role", "Rider", "Family Member"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = roles[position];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || selectedRole.equals("Select Role")) {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

                                userRef.get().addOnSuccessListener(snapshot -> {
                                    if (snapshot.exists()) {
                                        String roleInDB = snapshot.child("role").getValue(String.class);
                                        if (roleInDB != null && selectedRole.equals(roleInDB)) {

                                            SharedPreferences prefs = getSharedPreferences("HelmetHeroPrefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString("role", roleInDB);
                                            editor.apply();

                                            // ====== SAVE FCM TOKEN TO DB ======
                                            FirebaseMessaging.getInstance().getToken()
                                                    .addOnCompleteListener(tokenTask -> {
                                                        if (tokenTask.isSuccessful()) {
                                                            String token = tokenTask.getResult();
                                                            FirebaseDatabase.getInstance()
                                                                    .getReference("Users").child(uid).child("fcmToken")
                                                                    .setValue(token);
                                                        }
                                                    });
                                            // ===================================

                                            // Redirect to correct Home
                                            if (roleInDB.equals("Rider")) {
                                                startActivity(new Intent(LoginActivity.this, RiderHomeActivity.class));
                                            } else {
                                                startActivity(new Intent(LoginActivity.this, FamilyHomeActivity.class));
                                            }
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Selected role does not match your registered role.", Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Role not found in database.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        signupText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }
}