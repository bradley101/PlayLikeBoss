package com.bradley.playlikeboss;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by bradley on 21-12-2016.
 */

public class HomeScreen extends AppCompatActivity {

    Button streamBtn, connectBtn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_fragment);

        initializeElements();
        setListeners();
    }

    private void setListeners() {
        streamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectItem(1);
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectItem(2);
            }
        });
    }

    private void initializeElements() {
        streamBtn = (Button) findViewById(R.id.app_stream_music_btn);
        connectBtn = (Button) findViewById(R.id.app_connect_to_host_btn);
    }

    private void selectItem (int i) {
        Intent intent = null;
        switch (i) {
            case 1:
                intent = new Intent(HomeScreen.this, StreamSong.class);
                break;
            case 2:
                intent = new Intent(HomeScreen.this, ConnectDevice.class);
                break;
        }
        startActivity(intent);
    }

}
