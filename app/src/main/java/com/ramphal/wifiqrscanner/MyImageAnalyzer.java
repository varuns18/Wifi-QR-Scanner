package com.ramphal.wifiqrscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class MyImageAnalyzer implements ImageAnalysis.Analyzer {
    private final Context context;
    private boolean isScanning = false; // Flag to prevent multiple scans
    private final Handler handler = new Handler(Looper.getMainLooper());

    public MyImageAnalyzer(Context context) {
        this.context = context;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            scanBarcode(imageProxy);
        } else {
            imageProxy.close(); // Close the proxy if already scanning
        }
    }

    public void analyzeBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();

            BarcodeScanner scanner = BarcodeScanning.getClient(options);
            Task<List<Barcode>> result = scanner.process(image)
                    .addOnSuccessListener(this::readBarcodeData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed due to: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void scanBarcode(ImageProxy imageProxy) {
        if (imageProxy != null) {
            @SuppressLint("UnsafeOptInUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient(options);
                Task<List<Barcode>> result = scanner.process(image)
                        .addOnSuccessListener(this::readBarcodeData)
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Scanning failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }).addOnCompleteListener(task -> {
                            imageProxy.close(); // Ensure the image proxy is closed
                        });
            }
        }
    }

    private void readBarcodeData(List<Barcode> barcodes) {
        if (isScanning) return; // Prevent multiple scans
        isScanning = true; // Set scanning flag

        for (Barcode barcode : barcodes) {
            int valueType = barcode.getValueType();

            Intent intent = new Intent(context, Result.class);
            if (valueType == Barcode.TYPE_WIFI) {
                // Extract Wi-Fi details
                Barcode.WiFi wifiDetails = barcode.getWifi();
                String ssid = wifiDetails.getSsid(); // SSID
                String password = wifiDetails.getPassword(); // Password
                int encryptionType = wifiDetails.getEncryptionType(); // Encryption Type

                // Convert encryption type to readable format
                String encryptionTypeText;
                switch (encryptionType) {
                    case Barcode.WiFi.TYPE_OPEN:
                        encryptionTypeText = "Open";
                        break;
                    case Barcode.WiFi.TYPE_WEP:
                        encryptionTypeText = "WEP";
                        break;
                    case Barcode.WiFi.TYPE_WPA:
                        encryptionTypeText = "WPA/WPA2";
                        break;
                    default:
                        encryptionTypeText = "Unknown";
                }

                intent.putExtra("SSID", ssid);
                intent.putExtra("PASSWORD", password);
                intent.putExtra("ENCRYPTION_TYPE", encryptionTypeText);
            } else {
                // Handle other types of barcodes (URLs, Text, etc.)
                intent.putExtra("RAW_VALUE", barcode.getRawValue());
            }

            // Start the Result activity
            context.startActivity(intent);
            break; // Exit after processing the first barcode
        }

        // Reset the scanning flag after a delay
        handler.postDelayed(() -> isScanning = false, 2000); // 2 seconds delay
    }
}
