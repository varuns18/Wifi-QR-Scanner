package com.ramphal.wifiqrscanner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class Result extends AppCompatActivity {

    private TextView wifiNameTextView;
    private TextView wifiPasswordTextView;
    private TextView wifiSecurityTextView;
    private ImageView wifiQRCodeImageView;
    private DatabaseHelper databaseHelper;
    private long wifiId; // Store the Wi-Fi ID
    private InterstitialAd mInterstitialAd;
    private static final String TAG = "Result";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "AdCounterPref";
    private static final String KEY_AD_COUNTER = "ad_counter";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        checkAndShowAd();

        wifiNameTextView = findViewById(R.id.wifi_name);
        wifiPasswordTextView = findViewById(R.id.wifi_password);
        wifiSecurityTextView = findViewById(R.id.wifi_security);
        wifiQRCodeImageView = findViewById(R.id.wifi_qrcode);

        // Initialize buttons
        ExtendedFloatingActionButton deleteButton = findViewById(R.id.delete);
        ExtendedFloatingActionButton shareButton = findViewById(R.id.share);
        ExtendedFloatingActionButton copyButton = findViewById(R.id.copy);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);


        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Retrieve the intent extras
        wifiId = getIntent().getLongExtra("ID", -1); // Get ID from the intent

        if (wifiId != -1) {
            // Fetch the Wi-Fi details from the database
            DataModel wifiDetails = databaseHelper.getDataById(wifiId);

            if (wifiDetails != null) {
                // Display the Wi-Fi details
                wifiNameTextView.setText(wifiDetails.getSsid());
                wifiPasswordTextView.setText(wifiDetails.getPassword());
                wifiSecurityTextView.setText(wifiDetails.getEncryptionType());

                // Generate and display QR code
                generateQRCode(wifiDetails.getSsid(), wifiDetails.getPassword(), wifiDetails.getEncryptionType());
            } else {
                Toast.makeText(this, "Wi-Fi details not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle when no ID is passed (scan result)
            String ssid = getIntent().getStringExtra("SSID");
            String password = getIntent().getStringExtra("PASSWORD");
            String encryptionType = getIntent().getStringExtra("ENCRYPTION_TYPE");
            String rawValue = getIntent().getStringExtra("RAW_VALUE");

            if (ssid != null && password != null && encryptionType != null) {
                wifiNameTextView.setText(ssid);
                wifiPasswordTextView.setText(password);
                wifiSecurityTextView.setText(encryptionType);

                // Generate and display QR code
                generateQRCode(ssid, password, encryptionType);

                // Save the Wi-Fi details to the database and get the new Wi-Fi ID
                wifiId = databaseHelper.insertData(ssid, password, encryptionType); // Capture the inserted Wi-Fi ID
            } else if (rawValue != null) {
                wifiNameTextView.setText("Raw Value: " + rawValue);
                wifiPasswordTextView.setText("");
                wifiSecurityTextView.setText("");
            }
        }

        // Set up button functionalities
        deleteButton.setOnClickListener(v -> confirmDelete());
        shareButton.setOnClickListener(v -> shareWifiDetails());
        copyButton.setOnClickListener(v -> copyPasswordToClipboard());
        topAppBar.setNavigationOnClickListener(v -> onBackPressed());

    }

    private void checkAndShowAd() {
        // Get current ad counter value, default is 0
        int adCounter = sharedPreferences.getInt(KEY_AD_COUNTER, 0);

        if (adCounter == 0) {
            AdsServices.loadInterstitialAd(Result.this);
            AdsServices.showAd(Result.this);
        }
        adCounter += 1;
        if (adCounter == 2){
            adCounter = 0;
        }

        // Save the updated counter back to SharedPreferences
        editor.putInt(KEY_AD_COUNTER, adCounter);
        editor.apply(); // Apply changes
    }

    private void showAd() {
        // Your code to display the ad
        Toast.makeText(this, "Ad is being shown", Toast.LENGTH_LONG).show();
    }

    private void confirmDelete() {
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Delete Wi-Fi")
                .setMessage("Are you sure you want to delete this Wi-Fi details?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // If confirmed, delete the entry
                    deleteData();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteData() {
        if (wifiId != -1) {
            // Delete the Wi-Fi details from the database
            boolean isDeleted = databaseHelper.deleteData(wifiId);
            if (isDeleted) {
                Toast.makeText(this, "Wi-Fi details deleted successfully", Toast.LENGTH_SHORT).show();

                // Send the result back to HistoryActivity to indicate a successful deletion
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deletedId", wifiId);
                setResult(RESULT_OK, resultIntent);

                finish(); // Close the activity after deletion
            } else {
                Toast.makeText(this, "Failed to delete Wi-Fi details", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No Wi-Fi details to delete", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateQRCode(String ssid, String password, String encryptionType) {
        String qrContent = "WIFI:T:" + encryptionType + ";S:" + ssid + ";P:" + password + ";;";

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            Bitmap qrBitmap = toBitmap(qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300));
            wifiQRCodeImageView.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(com.google.zxing.common.BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int primaryColor = ContextCompat.getColor(this, R.color.primary);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? primaryColor : Color.TRANSPARENT);
            }
        }
        return bmp;
    }

    private void shareWifiDetails() {
        // Get Wi-Fi details
        String ssid = wifiNameTextView.getText().toString();
        String password = wifiPasswordTextView.getText().toString();
        String encryptionType = wifiSecurityTextView.getText().toString();

        // Create the share message
        String shareMessage = "Check out this Wi-Fi information:\n" +
                "SSID: " + ssid + "\n" +
                "Password: " + password + "\n" +
                "Encryption: " + encryptionType + "\n\n" +
                "Share your Wi-Fi with others using this app!\n" +
                "Download it here: https://play.google.com/store/apps/details?id=com.ramphal.wifiqrscanner"; // Replace with your actual app link

        // Create an intent to share the message
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share Wi-Fi Details"));
    }

    private void copyPasswordToClipboard() {
        String password = wifiPasswordTextView.getText().toString();

        // Get the clipboard manager
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Wi-Fi Password", password);
        clipboard.setPrimaryClip(clip);

        // Show a toast message to confirm the action
        Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
    }

}
