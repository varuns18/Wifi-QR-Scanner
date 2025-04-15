package com.ramphal.wifiqrscanner;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private Context context;
    private List<DataModel> dataList;

    public Adapter(Context context, List<DataModel> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        DataModel model = dataList.get(position);

        // Use getSsid() to display the Wi-Fi name
        holder.wifiName.setText(model.getSsid()); // SSID
        holder.wifiPassword.setText(model.getPassword()); // Password

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Result.class);
            intent.putExtra("ID", model.getId()); // Send the ID to fetch Wi-Fi details

            // Use startActivityForResult instead of startActivity
            ((history) context).startActivityForResult(intent, 100); // Pass requestCode = 100
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView wifiName;
        TextView wifiPassword; // Add field for password
        TextView wifiSecurity; // Add field for encryption type

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiName = itemView.findViewById(R.id.wifi_name);
            wifiPassword = itemView.findViewById(R.id.wifi_password); // Reference to password TextView
            wifiSecurity = itemView.findViewById(R.id.wifi_security); // Reference to encryption type TextView
        }
    }
}
