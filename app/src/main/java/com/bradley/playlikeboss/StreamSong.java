package com.bradley.playlikeboss;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by bradley on 21-12-2016.
 */

public class StreamSong extends AppCompatActivity {
    WifiConfiguration defaultWifiConfiguration, appWifiConfiguration;
    private TextView step1TextView;
    private Button step1Button;
    private Button step2Button;
    private Button playPauseButton;
    ArrayList<Socket> openClientSockets;
    final String TAG = "StreamSongActivity";
    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_stream);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadDefaultConfiguration();
        disableWifiHotspotIfPreviouslyEnabled();

        initializeElements();

        setListeners();
    }

    private void setListeners() {
        step1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (step1Button.getText().toString().toLowerCase()) {
                    case "activate":
                        setEnabledWifiHotspot(true);
                        step1Button.setText("Deactivate");
                        ((RelativeLayout) findViewById(R.id.layout_stream_step2_rl)).setVisibility(View.VISIBLE);
                        break;
                    case "deactivate":
                        setEnabledWifiHotspot(false);
                        step1Button.setText("Activate");
                        ((RelativeLayout) findViewById(R.id.layout_stream_step2_rl)).setVisibility(View.GONE);
                        ((RelativeLayout) findViewById(R.id.layout_stream_play_pause_rl)).setVisibility(View.GONE);
                        break;
                }
            }
        });

        step2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket s;
                        PrintWriter writer;
                        for (int i = 0 ; i < openClientSockets.size() ; i += 1) {
                            s = openClientSockets.get(i);
                            try {
                                writer = new PrintWriter(s.getOutputStream());
                                writer.print("play");
                                writer.flush();
                                writer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeElements() {
        step1TextView = (TextView) findViewById(R.id.layout_stream_step1tv);
        step1Button = (Button) findViewById(R.id.layout_stream_step1btn);
        step2Button = (Button) findViewById(R.id.layout_stream_step2btn);
        playPauseButton = (Button) findViewById(R.id.layout_stream_play_pause);

        ((RelativeLayout) findViewById(R.id.layout_stream_step2_rl)).setVisibility(View.GONE);
        ((RelativeLayout) findViewById(R.id.layout_stream_play_pause_rl)).setVisibility(View.GONE);
    }

    private boolean isWifiHotspotEnabled() {
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

    private void disableWifiHotspotIfPreviouslyEnabled() {
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

    private boolean setEnabledWifiHotspot(boolean set) {
        boolean status = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            if (set) {
                method.invoke(wifiManager, appWifiConfiguration, true);
            } else {
                try {
                    method.invoke(wifiManager, appWifiConfiguration, false);
                    Thread.sleep(100);
                    method.invoke(wifiManager, defaultWifiConfiguration, true);
                    Thread.sleep(100);
                    method.invoke(wifiManager, defaultWifiConfiguration, false);
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

    void activateWifiHotspot() {
        setEnabledWifiHotspot(true);
//        ((TextView) findViewById(R.id.layout_stream_step1tv)).setText(R.string.step12);
//        ((RelativeLayout) findViewById(R.id.layout_stream_step2_rl)).setVisibility(View.VISIBLE);
    }

    void deactivateHotspot() {
        setEnabledWifiHotspot(false);
//        ((TextView) findViewById(R.id.layout_stream_step1tv)).setText(R.string.step11);
    }

    void showFileChooser() {
        Intent fileChooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileChooserIntent.setType("*/*");
        fileChooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult (fileChooserIntent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                ((RelativeLayout) findViewById(R.id.layout_stream_play_pause_rl)).setVisibility(View.VISIBLE);
                Cursor cursor = getContentResolver().query(data.getData(),
                        new String[] {MediaStore.Audio.AudioColumns.DATA},
                        null,
                        null,
                        null
                );
                cursor.moveToFirst();
                String filePath = cursor.getString(0);
                cursor.close();
                loadFileToClients(new File(filePath));
            }
        }
    }

    private void loadFileToClients(final File file) {
        openClientSockets = new ArrayList<>();
        String filePath = file.getAbsolutePath();
        int indexOfDot = filePath.lastIndexOf(".");
        final String extension = filePath.substring(indexOfDot + 1, indexOfDot + 4);
        Thread clientListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(40404);
                    while (true) {
                        final Socket clientSocket = server.accept();

                        log("new client device connected - " + clientSocket.getInetAddress().toString());

                        openClientSockets.add(clientSocket);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    log("sending file to client - " + clientSocket.getInetAddress().toString());

                                    OutputStream outputStream = clientSocket.getOutputStream();
                                    PrintWriter writer = new PrintWriter(outputStream);
                                    writer.print(extension);
//                                    writer.close();

                                    InputStream fileInputStream = new FileInputStream(file);

                                    byte[] byteBuffer = new byte[16 * 1024];
                                    int count;
                                    while ((count = fileInputStream.read(byteBuffer)) > 0) {
                                        outputStream.write(byteBuffer, 0, count);
                                    }

                                    log("file sent to client - " + clientSocket.getInetAddress().toString());

                                    fileInputStream.close();
                                    outputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        clientListenerThread.start();

    }

    void log (String message) {
        Log.i(TAG, message);
    }
}
