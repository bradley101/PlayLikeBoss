package com.bradley.playlikeboss;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by bradley on 18-12-2016.
 */

public class SettingsActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    EditText ssid, pwd;
    Button save;
    CheckBox togglePwd;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences("com.bradley.playlikeboss.SETTINGS", MODE_PRIVATE);

        initializeElements();
        retainOriginalValues();
        setListeners();
    }

    private void setListeners() {
        togglePwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (togglePwd.isChecked()) {
                    pwd.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    pwd.setInputType(129);
                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inn = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inn.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ssid", ssid.getText().toString());
                editor.putString("key", pwd.getText().toString());
                editor.commit();
                Toast.makeText(getApplicationContext(), "Settings have been saved", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void retainOriginalValues() {
        ssid.setText(getSavedItem("ssid"));
        pwd.setText(getSavedItem("key"));

    }

    private void initializeElements() {
        ssid = (EditText) findViewById(R.id.settings_ssid_et);
        pwd = (EditText) findViewById(R.id.settings_key_et);
        save = (Button) findViewById(R.id.settings_save_btn);
        togglePwd = (CheckBox) findViewById(R.id.settings_key_toggle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getSavedItem(String key) {
        return sharedPreferences.getString(key, "");
    }
}
