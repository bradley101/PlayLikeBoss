package com.bradley.playlikeboss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by bradley on 21-12-2016.
 */

public class ConnectDevice extends AppCompatActivity {
    TextView connectStatus;
    BroadcastReceiver broadcastReceiver;
    ProgressBar progressBar;
    MediaPlayer mediaPlayer;
    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_connect);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mediaPlayer = new MediaPlayer();
        connectStatus = (TextView) findViewById(R.id.connect_status_tv);
        progressBar = (ProgressBar) findViewById(R.id.connect_song_load_status);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager.getActiveNetworkInfo().isConnected()) {
                    connectStatus.setText("Connected");
                    doAllTheWork();
                } else {
                    connectStatus.setText(R.string.cstep11);
                }
            }
        };
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

    private void doAllTheWork() {
        Thread listenToSongThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket clientSocket = new Socket("192.168.43.1", 40404);
                    InputStream inputStream = clientSocket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String ext = null;
                    while ((ext = bufferedReader.readLine()) != null) {
                        break;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectStatus.setText("Receiving song..");
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });

                    byte[] byteBuffer = new byte [16 * 1024];
                    OutputStream fileOutputStream = new FileOutputStream("song." + ext);
                    int count = 0;
                    while ((count = inputStream.read(byteBuffer)) > 0) {
                        fileOutputStream.write(byteBuffer, 0, count);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectStatus.setText("Song received!");
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    mediaPlayer.setDataSource("song." + ext);
                    mediaPlayer.prepareAsync();
                    String s = null;
                    boolean finish = false;
                    while ((s = bufferedReader.readLine()) != null) {
                        switch (s) {
                            case "pause":
                                mediaPlayer.pause();
                                break;
                            case "play":
                                mediaPlayer.start();
                                break;
                            case "stop":
                                mediaPlayer.stop();
                                break;
                            case "finish":
                                finish = true;
                                break;
                        }
                        s = null;
                        if (finish) break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        listenToSongThread.start();
    }
}
