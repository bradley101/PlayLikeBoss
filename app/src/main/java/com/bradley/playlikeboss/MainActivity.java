package com.bradley.playlikeboss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.FileObserver;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    TextView hotspotStatus, connectedDevices, wifiStatus;
    ToggleButton toggleHotspotButton;
    WifiConfiguration defaultWifiConfiguration, appWifiConfiguration;
    BroadcastReceiver broadcastReceiver, wifiBroadcastReceiver;
    FileObserver fileObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadDefaultConfiguration();
        disbaleWifiHotspotIfPreviouslyEnabled();

        initializeElements();
        setListeners();

        new SetTextView().execute();
        initializeWifiHotspotStatus();

        registerBroadcastReceiver();

        //setFileObserver();

    }



    private void disbaleWifiHotspotIfPreviouslyEnabled() {
        if (isWifiHotspotEnabled()) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            try {
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifiManager, defaultWifiConfiguration, false);
                Thread.sleep(100);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setFileObserver() {
        fileObserver = new FileObserver(new File("/proc/net/arp").getAbsolutePath()) {
            @Override
            public void onEvent(int i, String s) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
                    int count = 0;
                    while (br.readLine() != null) count++;
                    br.close();
                    String str = connectedDevices.getText().toString();
                    String pre = str.substring(0, str.indexOf(":") + 1);
                    str = pre + count;
                    connectedDevices.setText(str);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initializeWifiHotspotStatus();
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);

        IntentFilter wifiIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new SetTextView().execute();
            }
        };
        registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);
    }

    private void initializeWifiHotspotStatus() {
        boolean status = isWifiHotspotEnabled();
        if (status) {
            hotspotStatus.setText("Enabled");
            hotspotStatus.setTextColor(Color.GREEN);

        } else {
            hotspotStatus.setText("Disabled");
            hotspotStatus.setTextColor(Color.RED);
        }
    }

    private void setListeners() {
        toggleHotspotButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setEnabledWifiHotspot(b);
            }
        });
    }

    private void initializeElements() {
        hotspotStatus = (TextView) findViewById(R.id.status_hotspot_tv);
        toggleHotspotButton = (ToggleButton) findViewById(R.id.toggle_wifi_hotspot_state_button);
        connectedDevices = (TextView) findViewById(R.id.status_hotspot_connected_devices_tv);
        connectedDevices.setVisibility(View.GONE);
        wifiStatus = (TextView) findViewById(R.id.status_wifi_tv);
    }

    void loadDefaultConfiguration() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("getWifiApConfiguration")) {
                try {
                    WifiConfiguration wifiConfiguration = (WifiConfiguration) m.invoke(wifiManager);
                    this.defaultWifiConfiguration = wifiConfiguration;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void loadAppWifiHotspotConfiguration() {
        WifiConfiguration config = null;
        SharedPreferences preferences = getSharedPreferences("com.bradley.playlikeboss.SETTINGS", MODE_PRIVATE);
        String ssid, key;
        ssid = preferences.getString("ssid", "Tap to Connect - Play Like Boss");
        key = preferences.getString("key", "playlikeboss");

        config = new WifiConfiguration();
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.SSID = ssid;
        config.preSharedKey = key;

        if (!preferences.contains("ssid") && preferences.contains("key")) {
            // Toast to inform user of no user defined wifi hotspot settings
            Toast.makeText(getApplicationContext(), "Default Hotspot settings are loaded. See them in Settings.", Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("ssid", ssid);
            editor.putString("key", key);
            editor.commit();
        }

        this.appWifiConfiguration = config;
    }

    boolean isWifiHotspotEnabled() {
        Boolean status;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("isWifiApEnabled")) {
                try {
                    status = (Boolean) m.invoke(wifiManager);
                    return status;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private boolean setEnabledWifiHotspot(boolean set) {
        boolean status = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            if (set) {
                loadDefaultConfiguration();
                loadAppWifiHotspotConfiguration();
                method.invoke(wifiManager, appWifiConfiguration, true);
                //connectedDevices.setVisibility(View.VISIBLE);
                //fileObserver.startWatching();
            } else {
                try {
                    method.invoke(wifiManager, appWifiConfiguration, false);
                    Thread.sleep(100);
                    method.invoke(wifiManager, defaultWifiConfiguration, true);
                    Thread.sleep(100);
                    method.invoke(wifiManager, defaultWifiConfiguration, false);
                    //connectedDevices.setVisibility(View.GONE);
                    //fileObserver.stopWatching();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            status = true;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(wifiBroadcastReceiver);
    }

    class SetTextView extends AsyncTask<Object, Object, Object> {
        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            initializeWifiConnectionStatus();
        }

        @Override
        protected Object doInBackground(Object... objects) {
            return null;
        }
        private void initializeWifiConnectionStatus() {
            if (isWifiConnected()) {
                wifiStatus.setText("Connected");
                wifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                wifiStatus.setText("Disconnected");
                wifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }

        private boolean isWifiConnected() {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getBSSID() == null) {
                return false;
            }
            return true;
        }
    }
}
