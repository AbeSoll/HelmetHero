package com.example.helmethero.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, phoneInput, passwordInput;
    private Spinner roleSpinner;
    private Button signupButton;
    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameInput = findViewById(R.id.inputName);
        emailInput = findViewById(R.id.inputEmail);
        phoneInput = findViewById(R.id.inputPhone);
        passwordInput = findViewById(R.id.inputPassword);
        roleSpinner = findViewById(R.id.spinnerRole);
        signupButton = findViewById(R.id.btnSignup);

        TextView textLoginLink = findViewById(R.id.textLoginLink);
        textLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        signupButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // User is successfully created
                            Toast.makeText(SignupActivity.this, "✅ Registered successfully!", Toast.LENGTH_LONG).show();

                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();

                                HashMap<String, Object> userMap = new HashMap<>();
                                userMap.put("name", name);
                                userMap.put("email", email);
                                userMap.put("phone", phone);
                                userMap.put("role", role);

                                database.child(userId).setValue(userMap)
                                        .addOnFailureListener(e -> {
                                            // Handle failure to save data
                                            Toast.makeText(SignupActivity.this, "❌ Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            // Handle authentication errors
                            String error = task.getException().getMessage();
                            if (error != null && error.contains("email address is already in use")) {
                                Toast.makeText(SignupActivity.this, "❗ This email is already registered", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SignupActivity.this, "Sign up failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }
}