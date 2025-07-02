package com.example.helmethero.fragments.rider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.helmethero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import com.yalantis.ucrop.UCrop;

// ZXing for QR
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;

public class RiderProfileSetupFragment extends Fragment {

    private EditText inputName, inputPhone, inputEmail, inputRole;
    private Button btnSave, btnCancel, btnShowCode;
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
        btnShowCode = view.findViewById(R.id.btnShowCode);

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

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        if (sourceUri != null) {
                            Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_profile_rider.jpg"));
                            UCrop.Options options = new UCrop.Options();
                            options.setCircleDimmedLayer(true);
                            options.setShowCropFrame(false);
                            options.setShowCropGrid(false);
                            UCrop.of(sourceUri, destinationUri)
                                    .withAspectRatio(1, 1)
                                    .withOptions(options)
                                    .start(requireContext(), this);
                        }
                    }
                });

        profileImageView.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveUserProfile());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        btnShowCode.setOnClickListener(v -> showInviteCodeDialog());

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK && data != null) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                selectedImageUri = resultUri;
                Glide.with(requireContext())
                        .load(selectedImageUri)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImageView);
            }
        } else if (requestCode == UCrop.REQUEST_CROP) {
            Toast.makeText(getContext(), "❌ Image crop cancelled", Toast.LENGTH_SHORT).show();
        }
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

    // In your saveUserProfile() method
    @SuppressLint("SetTextI18n")
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
            // --- Start of corrected block ---

            // 1. Create a modern AlertDialog for progress
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setCancelable(false); // User cannot dismiss it

            // Create a horizontal layout to hold the ProgressBar and a message
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setPadding(30, 30, 30, 30);
            linearLayout.setGravity(Gravity.CENTER);

            ProgressBar progressBar = new ProgressBar(getContext());
            linearLayout.addView(progressBar);

            TextView messageView = new TextView(getContext());
            messageView.setText("Uploading profile image...");
            messageView.setPadding(20, 0, 0, 0);
            linearLayout.addView(messageView);

            builder.setView(linearLayout);
            AlertDialog dialog = builder.create();
            dialog.show();

            // --- End of corrected block ---

            StorageReference imageRef = storageRef.child(currentUser.getUid() + "/profile.jpg");

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                userRef.child("profileImageUrl").setValue(uri.toString());
                                dialog.dismiss(); // Dismiss the modern dialog
                                Toast.makeText(getContext(), "✅ Profile Updated!", Toast.LENGTH_SHORT).show();
                            })
                    )
                    .addOnFailureListener(e -> {
                        dialog.dismiss(); // Dismiss the modern dialog
                        Toast.makeText(getContext(), "❌ Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "✅ Profile Updated!", Toast.LENGTH_SHORT).show();
        }
    }

    // Dialog for Invite Code/QR (improved chip tab look)
    @SuppressLint("SetTextI18n")
    private void showInviteCodeDialog() {
        Context context = requireContext();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Layout for dialog (manual inflate, modern tabs)
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 48, 36, 18);

        // Chip-style tab bar (TextView)
        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(0, 0, 0, 28);

        TextView btnTabCode = new TextView(context);
        btnTabCode.setText("Code");
        btnTabCode.setTypeface(null, Typeface.BOLD);
        btnTabCode.setGravity(Gravity.CENTER);
        btnTabCode.setTextSize(16);
        btnTabCode.setBackgroundResource(R.drawable.tab_selector);
        btnTabCode.setTextColor(Color.WHITE);
        btnTabCode.setSelected(true);

        TextView btnTabQR = new TextView(context);
        btnTabQR.setText("QR");
        btnTabQR.setTypeface(null, Typeface.BOLD);
        btnTabQR.setGravity(Gravity.CENTER);
        btnTabQR.setTextSize(16);
        btnTabQR.setBackgroundResource(R.drawable.tab_selector);
        btnTabQR.setTextColor(Color.parseColor("#1E2D60"));
        btnTabQR.setSelected(false);

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, 88, 1f);
        tabParams.setMargins(8, 0, 8, 0);
        tabBar.addView(btnTabCode, tabParams);
        tabBar.addView(btnTabQR, tabParams);
        layout.addView(tabBar);

        // Code layout
        LinearLayout codeLayout = new LinearLayout(context);
        codeLayout.setOrientation(LinearLayout.VERTICAL);
        codeLayout.setPadding(0, 24, 0, 0);

        TextView codeView = new TextView(context);
        codeView.setText(uid);
        codeView.setTextSize(20f);
        codeView.setTextColor(Color.BLACK);
        codeView.setPadding(0, 10, 0, 26);
        codeView.setGravity(Gravity.CENTER_HORIZONTAL);

        Button btnCopy = new Button(context);
        btnCopy.setText("Copy");
        btnCopy.setAllCaps(false);
        btnCopy.setBackgroundResource(R.drawable.rounded_button_blue);
        btnCopy.setTextColor(Color.WHITE);

        codeLayout.addView(codeView);
        codeLayout.addView(btnCopy);

        // QR layout
        LinearLayout qrLayout = new LinearLayout(context);
        qrLayout.setOrientation(LinearLayout.VERTICAL);
        qrLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        qrLayout.setPadding(0, 20, 0, 0);

        ImageView qrImage = new ImageView(context);
        qrImage.setPadding(0, 26, 0, 26);
        qrImage.setAdjustViewBounds(true);

        Bitmap qrBitmap = generateQRCodeBitmap(uid, 540);
        if (qrBitmap != null) {
            qrImage.setImageBitmap(qrBitmap);
        }

        TextView qrCaption = new TextView(context);
        qrCaption.setText("Family can scan this QR to add you.");
        qrCaption.setGravity(Gravity.CENTER_HORIZONTAL);
        qrCaption.setPadding(0, 14, 0, 0);
        qrCaption.setTextSize(15);

        qrLayout.addView(qrImage);
        qrLayout.addView(qrCaption);

        // Container for both views, toggle by tab
        FrameLayout contentFrame = new FrameLayout(context);
        contentFrame.addView(codeLayout);
        contentFrame.addView(qrLayout);
        qrLayout.setVisibility(View.GONE); // Start with code tab

        layout.addView(contentFrame);

        // Tab logic (chip look)
        btnTabCode.setOnClickListener(v -> {
            btnTabCode.setSelected(true);
            btnTabCode.setTextColor(Color.WHITE);
            btnTabQR.setSelected(false);
            btnTabQR.setTextColor(Color.parseColor("#1E2D60"));
            codeLayout.setVisibility(View.VISIBLE);
            qrLayout.setVisibility(View.GONE);
        });

        btnTabQR.setOnClickListener(v -> {
            btnTabQR.setSelected(true);
            btnTabQR.setTextColor(Color.WHITE);
            btnTabCode.setSelected(false);
            btnTabCode.setTextColor(Color.parseColor("#1E2D60"));
            codeLayout.setVisibility(View.GONE);
            qrLayout.setVisibility(View.VISIBLE);
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Invite Code", uid);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Invite code copied!", Toast.LENGTH_SHORT).show();
        });

        new AlertDialog.Builder(context)
                .setTitle("Share Invite")
                .setView(layout)
                .setPositiveButton("Close", null)
                .show();
    }

    // -- QR CODE GENERATION
    private Bitmap generateQRCodeBitmap(String content, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }
}