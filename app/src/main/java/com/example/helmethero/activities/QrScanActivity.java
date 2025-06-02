package com.example.helmethero.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * QR Code scanner Activity for HelmetHero using ZXing JourneyApps library.
 * Returns result via "QR_RESULT" in intent extra.
 */
public class QrScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(true); // Lock ikut orientation activity
        integrator.setCaptureActivity(CapturePortraitActivity.class); // PENTING: custom activity!
        integrator.setPrompt("Scan a HelmetHero Invite QR");
        integrator.setBeepEnabled(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE); // QR only
        integrator.setCameraId(0); // Use back camera
        integrator.setBarcodeImageEnabled(false); // No need to save images
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Parse ZXing scan result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String scannedUid = result.getContents();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("QR_RESULT", scannedUid);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
