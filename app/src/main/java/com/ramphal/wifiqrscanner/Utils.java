package com.ramphal.wifiqrscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utils {

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 123;
    private static int requestTimes = 0;

    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void cameraPermissionRequest(Context context, Runnable positiveAction) {
        String cameraPermission = Manifest.permission.CAMERA;

        if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED)
        {
            positiveAction.run();
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, cameraPermission)) {

            if (requestTimes < 1){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Camera Permission Required");
                builder.setMessage("Without accessing the camera, it is not possible to SCAN QR Codes...");
                builder.setPositiveButton("Allow Camera", (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{cameraPermission},
                            CAMERA_PERMISSION_REQUEST_CODE
                    );
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> {});
                builder.show();
                requestTimes++;
            } else {
                showPermissionDeniedDialog(context);
            }

        } else {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{cameraPermission},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    public static void showPermissionDeniedDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Permission Denied");
        builder.setMessage("Camera permission has been permanently denied. Please enable it in settings.");
        builder.setPositiveButton("Open Settings", (dialog, which) -> openPermissionSettings(context));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public static void openPermissionSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }
}
