package com.ramphal.wifiqrscanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.appbar.MaterialToolbar; // Ensure this import is used
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class history extends AppCompatActivity {

    RecyclerView rvHistory;
    TextView noDataText; // TextView to show no data message
    DatabaseHelper databaseHelper;
    List<DataModel> dataList;
    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        rvHistory = findViewById(R.id.rvHistory);
        noDataText = findViewById(R.id.noDataText); // Initialize the no data TextView

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Set up RecyclerView with a LinearLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvHistory.setLayoutManager(layoutManager);

        // Fetch all data from the database
        dataList = databaseHelper.getAllData();

        // Set up the adapter
        adapter = new Adapter(this, dataList);
        rvHistory.setAdapter(adapter);

        // Show or hide the "No Data" message
        updateNoDataMessage();

        // Initialize the MaterialToolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete) {
                clearAllHistory();
                return true;
            }
            return false;
        });
    }

    private void clearAllHistory() {
        // Clear all data from the database
        databaseHelper.clearAllData();

        // Fetch new data from the database
        dataList = databaseHelper.getAllData(); // Repopulate dataList
        adapter = new Adapter(this, dataList); // Reinitialize adapter with new data
        rvHistory.setAdapter(adapter); // Set new adapter

        // Update the no data message
        updateNoDataMessage();

        // Show a confirmation message
        Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show();
    }

    // Method to update the no data message visibility
    private void updateNoDataMessage() {
        if (dataList.isEmpty()) {
            noDataText.setVisibility(TextView.VISIBLE); // Show the no data message
            rvHistory.setVisibility(RecyclerView.GONE); // Hide the RecyclerView
        } else {
            noDataText.setVisibility(TextView.GONE); // Hide the no data message
            rvHistory.setVisibility(RecyclerView.VISIBLE); // Show the RecyclerView
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Check if the result contains the deleted ID
            long deletedId = data.getLongExtra("deletedId", -1);
            if (deletedId != -1) {
                // Refresh the list after deletion
                dataList = databaseHelper.getAllData();
                adapter = new Adapter(this, dataList);
                rvHistory.setAdapter(adapter); // Reset adapter with new data

                // Update the no data message
                updateNoDataMessage();
            }
        }
    }
}
