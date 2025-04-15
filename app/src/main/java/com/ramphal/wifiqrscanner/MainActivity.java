package com.ramphal.wifiqrscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.widget.ImageView;


public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private boolean isFlashOn = false; // Move this to class-level
    private PreviewView cameraView; // Declare cameraView here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        MaterialButton btnGallery = findViewById(R.id.btn_gallery);  // Replace with the actual id of the gallery button
        MaterialButton btnFlash = findViewById(R.id.btn_flash);      // Replace with the actual id of the flash button
        MaterialButton history = findViewById(R.id.history);
        cameraView = findViewById(R.id.camera_view); // Initialize cameraView

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, history.class);
                startActivity(intent);
            }
        });

        // Set up permission launcher
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Start Scanning.
                        startScanning(cameraView);
                    } else {
                        // Explain to the user why permission is needed
                        Utils.cameraPermissionRequest(this, () -> Utils.openPermissionSettings(this));
                        startScanning(cameraView);
                    }
                });

        // Set up gallery launcher
        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri imageUri = data.getData();
                            if (imageUri != null) {
                                performScanning(imageUri);
                            }
                        }
                    }
                }
        );

        // Set up animation for gallery button
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);

        // Open gallery on click
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // Set up flash button listener
        btnFlash.setOnClickListener(v -> navigateFlash(camera));

        // Request camera permission and start scanner
        requestCameraAndStartScanner(requestPermissionLauncher, cameraView);
    }

    private void requestCameraAndStartScanner(ActivityResultLauncher<String> requestPermissionLauncher, PreviewView cameraView) {
        String permission = Manifest.permission.CAMERA;

        if (Utils.isPermissionGranted(this, permission)) {
            // Start the scanner
            startScanning(cameraView);
        } else {
            // Request permission
            requestCameraPermission(requestPermissionLauncher);
        }
    }

    private void startScanning(PreviewView cameraView) {
        ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider, cameraView, cameraExecutor);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(ProcessCameraProvider cameraProvider, PreviewView cameraView, ExecutorService cameraExecutor) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());

        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Updated line to create MyImageAnalyzer with Activity context
        MyImageAnalyzer analyzer = new MyImageAnalyzer(this);
        imageAnalysis.setAnalyzer(cameraExecutor, analyzer);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    private void requestCameraPermission(ActivityResultLauncher<String> requestPermissionLauncher) {
        String permission = Manifest.permission.CAMERA;

        // Check if the Android version is 23 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(permission)) {
                // Show a custom explanation or dialog to explain why the permission is needed
                Utils.cameraPermissionRequest(this, () -> requestPermissionLauncher.launch(permission));
            } else if (!Utils.isPermissionGranted(this, permission)) {
                // Permission has been permanently denied, show an option to open app settings
                Utils.cameraPermissionRequest(this, () -> Utils.openPermissionSettings(this));
            } else {
                // Permission hasn't been permanently denied yet, request it
                requestPermissionLauncher.launch(permission);
            }
        } else {
            // For devices below API level 23, directly request the permission
            requestPermissionLauncher.launch(permission);
        }
    }

    private void performScanning(Uri imageUri) {
        Bitmap bitmap = loadBitmapFromUri(imageUri);
        if (bitmap != null) {
            MyImageAnalyzer analyzer = new MyImageAnalyzer(this); // Pass Activity context
            analyzer.analyzeBitmap(bitmap);
        }
    }

    private Bitmap loadBitmapFromUri(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateFlash(Camera camera) {
        MaterialButton btnFlash = findViewById(R.id.btn_flash);  // Make sure to cast it to MaterialButton

        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            if (isFlashOn) {
                camera.getCameraControl().enableTorch(false);
                btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.flash_off)); // Change the icon to flash_off
                isFlashOn = false;
            } else {
                camera.getCameraControl().enableTorch(true);
                btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.flash_on));  // Change the icon to flash_on
                isFlashOn = true;
            }
        }
    }

}
