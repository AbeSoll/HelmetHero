package com.example.helmethero.fragments.family;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class FamilyProfileSetupFragment extends Fragment {

    private EditText inputName, inputPhone, inputEmail, inputRole;
    private Button btnSave, btnCancel;
    private ImageView profileImageView;
    private Uri selectedImageUri;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // --- NEW: Modern way to handle Activity Results ---
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> ucropLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Initialize the launchers in onCreate ---
        initializeLaunchers();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_profile_setup, container, false);

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

        profileImageView.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveUserProfile());

        // --- REFACTORED: Use modern navigation instead of deprecated onBackPressed ---
        btnCancel.setOnClickListener(v -> {
            // This is a more robust way to go back that doesn't rely on NavComponent
            requireActivity().getSupportFragmentManager().popBackStack();
        });


        return view;
    }

    private void initializeLaunchers() {
        // Launcher for picking an image from the gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // Image selected, now start the cropper
                        startCrop(uri);
                    }
                });

        // Launcher for getting the result from UCrop
        ucropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            selectedImageUri = resultUri;
                            Glide.with(requireContext())
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(profileImageView);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        Toast.makeText(getContext(), "❌ Crop error: " + (cropError != null ? cropError.getMessage() : ""), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        // Use the new launcher to pick an image
        imagePickerLauncher.launch("image/*");
    }

    private void startCrop(@NonNull Uri sourceUri) {
        String destinationFileName = "cropped_profile_family.jpg";
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCompressionQuality(90);

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withOptions(options)
                .getIntent(requireContext());

        // Use the ucropLauncher to start the crop activity and get the result
        ucropLauncher.launch(cropIntent);
    }

    // --- DELETED: onActivityResult is no longer needed. Its logic is now in the launchers. ---
    // @Override
    // public void onActivityResult(...) { ... }

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
                                .into(profileImageView);
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
            // --- REFACTORED: Use a modern AlertDialog instead of ProgressDialog ---
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setCancelable(false); // User can't dismiss it

            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(40, 40, 40, 40);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            ProgressBar progressBar = new ProgressBar(getContext());
            layout.addView(progressBar);

            TextView message = new TextView(getContext());
            message.setText("Uploading profile image...");
            message.setPadding(20, 0, 0, 0);
            layout.addView(message);

            builder.setView(layout);
            AlertDialog dialog = builder.create();
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