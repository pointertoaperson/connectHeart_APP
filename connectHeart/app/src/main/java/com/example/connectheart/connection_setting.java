package com.example.connectheart;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class connection_setting extends AppCompatActivity {

    //define shared preferences variables
    private static final String PREFS_NAME = "ConnectHeartPrefs";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_BROKER = "broker";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PSDCONN = "PsdConn";
    private static final String KEY_GROUP = "group";


    EditText ssidEditText, passwordEditText, brokerEditText, deviceIdEditText, usernameEditText,PsdConnEditText,groupEditText;
    Button setButton, scanWifiButton;

    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;

    private static final int LOCATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);



        ssidEditText = findViewById(R.id.ssid);
        passwordEditText = findViewById(R.id.password);
        brokerEditText = findViewById(R.id.broker);
        deviceIdEditText = findViewById(R.id.deviceid);
        setButton = findViewById(R.id.set);
        scanWifiButton = findViewById(R.id.scan);
        usernameEditText = findViewById(R.id.username);
        PsdConnEditText = findViewById(R.id.PsdConn);
        groupEditText = findViewById(R.id.group);


        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    this,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Load encrypted values
            ssidEditText.setText(prefs.getString(KEY_SSID, ""));
            passwordEditText.setText(prefs.getString(KEY_PASSWORD, ""));
            brokerEditText.setText(prefs.getString(KEY_BROKER, ""));
            deviceIdEditText.setText(prefs.getString(KEY_DEVICE_ID, ""));
            usernameEditText.setText(prefs.getString(KEY_USERNAME, "null"));
            PsdConnEditText.setText(prefs.getString(KEY_PSDCONN, "null"));
            groupEditText.setText(prefs.getString(KEY_GROUP,""));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Allow network on main thread
       // StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        // Request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            setupListeners();
        }
    }

    private void setupListeners() {
        scanWifiButton.setOnClickListener(v -> startWifiScan());
        setButton.setOnClickListener(view -> sendPostRequest());
    }

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wi-Fi is disabled... Enabling it.", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            Toast.makeText(this, "Enable GPS for Wi-Fi scan", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        Toast.makeText(this, "Scanning Wi-Fi networks...", Toast.LENGTH_SHORT).show();

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                unregisterReceiver(this);

                if (!updated) {
                    Toast.makeText(context, "Scan not updated. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ScanResult> results = wifiManager.getScanResults();
                List<String> ssidList = new ArrayList<>();

                for (ScanResult result : results) {
                    if (!ssidList.contains(result.SSID) && !result.SSID.isEmpty()) {
                        ssidList.add(result.SSID);
                    }
                }

                if (ssidList.isEmpty()) {
                    Toast.makeText(context, "No networks found", Toast.LENGTH_SHORT).show();
                } else {
                    showSSIDPicker(ssidList);
                }
            }
        };

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);

        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "Scan failed to start. Try again.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showSSIDPicker(List<String> ssidList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Wi-Fi Network");

        CharSequence[] items = ssidList.toArray(new CharSequence[0]);
        builder.setItems(items, (dialog, which) -> ssidEditText.setText(ssidList.get(which)));

        builder.show();
    }

    private void sendPostRequest() {
        String ssid = ssidEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String broker = brokerEditText.getText().toString();
        String deviceId = deviceIdEditText.getText().toString();
        String username = usernameEditText.getText().toString();
        String psdconn = PsdConnEditText.getText().toString();
        String group = groupEditText.getText().toString();

        // Save preferences (can stay on main thread)
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences.Editor editor = EncryptedSharedPreferences.create(
                    this,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).edit();

            editor.putString(KEY_SSID, ssid);
            editor.putString(KEY_PASSWORD, password);
            editor.putString(KEY_BROKER, broker);
            editor.putString(KEY_DEVICE_ID, deviceId);
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PSDCONN, psdconn);
            editor.putString(KEY_GROUP,group);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Secure save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Perform network request in background thread
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/SetConfig");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String postData = "ssid=" + ssid +
                        "&password=" + password +
                        "&broker=" + broker +
                        "&device_id=" + deviceId +
                        "&username=" + username +
                        "&psdconn=" + psdconn +
                        "&group=" + group +
                        "&";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(connection_setting.this, "Wi-Fi config sent successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(connection_setting.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(connection_setting.this, "Failed: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(connection_setting.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupListeners();
            } else {
                Toast.makeText(this, "Location permission is required for Wi-Fi scanning", Toast.LENGTH_LONG).show();
            }
        }
    }
}
