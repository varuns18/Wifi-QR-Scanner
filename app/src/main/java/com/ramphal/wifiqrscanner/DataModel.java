package com.ramphal.wifiqrscanner;

public class DataModel {
    private long id;                  // ID of the entry
    private String ssid;              // Wi-Fi Name
    private String password;           // Wi-Fi Password
    private String encryptionType;     // Encryption Type

    // Constructor
    public DataModel(long id, String ssid, String password, String encryptionType) {
        this.id = id;
        this.ssid = ssid;
        this.password = password;
        this.encryptionType = encryptionType;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getSsid() {           // Renamed to getSsid()
        return ssid;
    }

    public String getPassword() {
        return password;
    }

    public String getEncryptionType() {
        return encryptionType;
    }
}
