package com.example.helmethero.fragments.rider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

public class RiderProfileSetupFragment extends Fragment {

    private EditText inputName, inputPhone, inputEmail, inputRole;
    private Button btnSave, btnCancel;
    private ImageView profileImageView;
    private Uri selectedImageUri;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_profile_setup, container, false);

        inputName = view.findViewById(R.id.editName);
        inputPhone = view.findViewById(R.id.editPhone);
        inputEmail = view.findViewById(R.id.editEmail);
        inputRole = view.findViewById(R.id.editRole);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        btnCancel = view.findViewById(R.id.btnCancelProfile);
        profileImageView = view.findViewById(R.id.imgProfile);

        inputEmail.setEnabled(false);
        inputRole.setEnabled(false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid());

            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference("profile_images");

            loadUserProfile();
        }

        // 1. Setup image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        if (selectedImageUri != null) {
                            Glide.with(requireContext())
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.ic_profile) // fallback
                                    .error(R.drawable.ic_profile)       // error fallback
                                    .circleCrop()
                                    .into(profileImageView);
                        } else {
                            Toast.makeText(getContext(), "❌ Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "⚠️ Image selection cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        // 2. Click listeners
        profileImageView.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveUserProfile());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    inputName.setText(snapshot.child("name").getValue(String.class));
                    inputPhone.setText(snapshot.child("phone").getValue(String.class));
                    inputEmail.setText(snapshot.child("email").getValue(String.class));
                    inputRole.setText(snapshot.child("role").getValue(String.class));

                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .circleCrop()
                                .into(profileImageView); // ✅ circular!
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String name = inputName.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.child("name").setValue(name);
        userRef.child("phone").setValue(phone);

        if (selectedImageUri != null) {
            ProgressDialog dialog = new ProgressDialog(getContext());
            dialog.setMessage("Uploading profile image...");
            dialog.setCancelable(false);
            dialog.show();

            StorageReference imageRef = storageRef.child(currentUser.getUid() + "/profile.jpg");

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                userRef.child("profileImageUrl").setValue(uri.toString());
                                dialog.dismiss();
                                Toast.makeText(getContext(), "✅ Profile Updated!", Toast.LENGTH_SHORT).show();
                            })
                    )
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "❌ Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "✅ Profile Updated!", Toast.LENGTH_SHORT).show();
        }
    }
}