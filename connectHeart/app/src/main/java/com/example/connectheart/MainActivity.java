package com.example.connectheart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import android.widget.LinearLayout;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.eclipse.paho.client.mqttv3.*;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity {

    private LinearLayout deviceDataContainer;
    private Map<String, TextView> deviceViews = new HashMap<>();


    private static final String PREFS_NAME = "ConnectHeartPrefs";
    private static final String TAG = "MQTT";
    private static String MQTT_BROKER_URL;
    private static String MQTT_TOPIC_SUB;
    private static String DEVICE_ID;
    private static String GROUP;

    private MqttClient mqttClient;
    private TextView mqttTextView, bannerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        deviceDataContainer = findViewById(R.id.deviceDataContainer);


        Button buttonGoToOther = findViewById(R.id.GoToSetting);
        buttonGoToOther.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, connection_setting.class);
            startActivity(intent);
        });

        mqttTextView = findViewById(R.id.mqttdata);
        bannerTextView = findViewById(R.id.banner);
        mqttTextView.setText("App started");
        bannerTextView.setText("GROUP");

        Context context = getApplicationContext();

        new Thread(() -> {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                SharedPreferences prefs = EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                MQTT_BROKER_URL = prefs.getString("broker", "");
                GROUP = prefs.getString("group", "test");

                MQTT_TOPIC_SUB = GROUP + "/#" ; //subscrive to all level

                runOnUiThread(() -> bannerTextView.setText(GROUP));

                Log.d(TAG, "Broker: " + MQTT_BROKER_URL + ", Topic: " + MQTT_TOPIC_SUB);

                // Now connect to MQTT
                new Thread(() -> {
                    try {
                        String clientId = MqttClient.generateClientId();
                        mqttClient = new MqttClient(MQTT_BROKER_URL, clientId, new MemoryPersistence());

                        MqttConnectOptions options = new MqttConnectOptions();
                        options.setCleanSession(true);
                        mqttClient.connect(options);

                        runOnUiThread(() -> mqttTextView.setText("Connected"));

                        mqttClient.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {
                                Log.e(TAG, "Connection lost", cause);
                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {
                                String msg = message.toString();
                                Log.d(TAG, "Received topic: " + topic + ", message: " + msg);

                                String[] tokens = topic.split("/");

                                if (tokens.length >= 3) {
                                    String deviceIdFromTopic = tokens[1];
                                    String lastLevel = tokens[tokens.length - 1];
                                    String displayText = deviceIdFromTopic + " :: " + msg;

                                    runOnUiThread(() -> {
                                        //mqttTextView.setText(displayText);

                                        if (!deviceViews.containsKey(deviceIdFromTopic)) {
                                            TextView deviceTextView = new TextView(MainActivity.this);
                                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    300
                                            );
                                            params.setMargins(0, 0, 0, 24);
                                            deviceTextView.setLayoutParams(params);
                                            deviceTextView.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_rounded));
                                            deviceTextView.setGravity(Gravity.CENTER);
                                            deviceTextView.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_rounded));
                                            deviceTextView.setGravity(Gravity.CENTER);
                                            deviceTextView.setTextColor(mqttTextView.getCurrentTextColor());
                                            deviceTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mqttTextView.getTextSize());
                                            deviceTextView.setTypeface(mqttTextView.getTypeface());





                                            deviceTextView.setText(displayText);

                                            deviceViews.put(deviceIdFromTopic, deviceTextView);
                                            deviceDataContainer.addView(deviceTextView);
                                        } else {
                                            deviceViews.get(deviceIdFromTopic).setText(displayText);
                                        }
                                    });

                                } else {
                                    Log.d(TAG, "Topic format unexpected: " + topic);
                                }
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {
                                // Optional log
                            }
                        });

                        mqttClient.subscribe(MQTT_TOPIC_SUB);

                    } catch (Exception e) {
                        Log.e(TAG, "MQTT error", e);
                        runOnUiThread(() -> mqttTextView.setText("MQTT error: " + e.getMessage()));
                    }
                }).start();

            } catch (Exception e) {
                Log.e(TAG, "Preference error", e);
                runOnUiThread(() -> mqttTextView.setText("Error: " + e.getMessage()));
            }
        }).start();
    }
}
