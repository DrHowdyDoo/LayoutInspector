package com.drhowdydoo.layoutinspector;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);
        TextView tvOpenAssistSetting = findViewById(R.id.tvOpenAssistSetting);
        TextView tvInfoStep2 = findViewById(R.id.info_step_2);
        TextView tvInfoStep3 = findViewById(R.id.info_step_3);
        TextView tvInfoStep4 = findViewById(R.id.info_step_4);
        setBoldText(tvInfoStep2,7,tvInfoStep2.getText().length());
        setBoldText(tvInfoStep3,7,27);
        setBoldText(tvInfoStep4,7,tvInfoStep4.getText().length() - 10);
        tvOpenAssistSetting.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        });

        String assistant= Settings.Secure.getString(getContentResolver(), "voice_interaction_service");
        boolean areWeGood=false;
        if (assistant!=null) {
            ComponentName cn=ComponentName.unflattenFromString(assistant);
            if (cn != null && cn.getPackageName().equals(getPackageName())) {
                areWeGood = true;
            }
        }


    }

    private void setBoldText(TextView tv, int start, int end){
        SpannableStringBuilder str = new SpannableStringBuilder(tv.getText());
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(str);
    }
}