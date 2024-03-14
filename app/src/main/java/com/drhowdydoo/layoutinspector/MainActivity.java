package com.drhowdydoo.layoutinspector;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);

        String assistant=
                Settings.Secure.getString(getContentResolver(),
                        "voice_interaction_service");

        boolean areWeGood=false;

        if (assistant!=null) {
            ComponentName cn=ComponentName.unflattenFromString(assistant);

            if (cn != null && cn.getPackageName().equals(getPackageName())) {
                areWeGood = true;
            }
        }

        if (!areWeGood) {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        }
    }
}