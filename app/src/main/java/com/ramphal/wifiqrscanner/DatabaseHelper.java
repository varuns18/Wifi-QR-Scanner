package com.ramphal.wifiqrscanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "history.db";
    private static final int DB_VERSION = 2; // Incremented version
    private static final String TABLE_NAME = "history";

    // Column names
    private static final String ID_COL = "id";
    private static final String SSID_COL = "ssid"; // New column for Wi-Fi name
    private static final String PASSWORD_COL = "password"; // New column for password
    private static final String ENCRYPTION_TYPE_COL = "encryption_type"; // New column for encryption type

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SSID_COL + " TEXT, " + // Wi-Fi Name
                PASSWORD_COL + " TEXT, " + // Wi-Fi Password
                ENCRYPTION_TYPE_COL + " TEXT)"; // Encryption Type
        db.execSQL(query);
    }

    // Change the return type to long
    public long insertData(String ssid, String password, String encryptionType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SSID_COL, ssid);
        values.put(PASSWORD_COL, password);
        values.put(ENCRYPTION_TYPE_COL, encryptionType);

        // Insert the data and return the row ID of the newly inserted row
        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id; // Return the new ID
    }

    public List<DataModel> getAllData() {
        List<DataModel> dataList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {ID_COL, SSID_COL, PASSWORD_COL, ENCRYPTION_TYPE_COL}; // Fetching new columns
        Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COL));
                String ssid = cursor.getString(cursor.getColumnIndexOrThrow(SSID_COL));
                String password = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_COL));
                String encryptionType = cursor.getString(cursor.getColumnIndexOrThrow(ENCRYPTION_TYPE_COL));

                dataList.add(new DataModel(id, ssid, password, encryptionType)); // Adjust DataModel constructor
            }
            cursor.close();
        }
        db.close();
        return dataList;
    }

    public DataModel getDataById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {ID_COL, SSID_COL, PASSWORD_COL, ENCRYPTION_TYPE_COL};
        Cursor cursor = db.query(TABLE_NAME, columns, ID_COL + "=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            long dbId = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COL));
            String ssid = cursor.getString(cursor.getColumnIndexOrThrow(SSID_COL));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_COL));
            String encryptionType = cursor.getString(cursor.getColumnIndexOrThrow(ENCRYPTION_TYPE_COL));
            cursor.close();
            return new DataModel(dbId, ssid, password, encryptionType); // Adjust DataModel constructor
        }

        return null; // Return null if no data found
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void clearTable() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("DatabaseHelper", "Clearing all data from " + TABLE_NAME);
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }



    public boolean deleteData(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, ID_COL + " = ?", new String[]{String.valueOf(id)}) > 0; // Use ID_COL for consistency
    }
}
