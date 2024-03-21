package com.drhowdydoo.layoutinspector;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

public class MainActivity extends AppCompatActivity {

    private boolean ACTIVITY_RESUMED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setViewContent();
    }

    private void setViewContent() {
        if (!areWeGood()) {
            setContentView(R.layout.activity_main);
            setUpMainGroup();
        } else {
            setContentView(R.layout.activity_main_2);
        }
    }

    private void setUpMainGroup() {
        TextView tvOpenAssistSetting = findViewById(R.id.tvOpenAssistSetting);
        TextView tvInfoStep2 = findViewById(R.id.info_step_2);
        TextView tvInfoStep3 = findViewById(R.id.info_step_3);
        TextView tvInfoStep4 = findViewById(R.id.info_step_4);
        setColoredBoldText(tvOpenAssistSetting, 5, tvOpenAssistSetting.getText().length());
        setBoldText(tvInfoStep2, 7, tvInfoStep2.getText().length());
        setBoldText(tvInfoStep3, 7, 27);
        setBoldText(tvInfoStep4, 7, tvInfoStep4.getText().length() - 10);
        tvOpenAssistSetting.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)));
    }

    private void setBoldText(TextView tv, int start, int end) {
        SpannableStringBuilder str = new SpannableStringBuilder(tv.getText());
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(str);
    }

    private void setColoredBoldText(TextView tv, int start, int end){
        SpannableStringBuilder str = new SpannableStringBuilder(tv.getText());
        int colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLUE);
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(new ForegroundColorSpan(colorPrimary), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(str);
    }

    private boolean areWeGood() {
        String assistant = Settings.Secure.getString(getContentResolver(), "voice_interaction_service");
        boolean areWeGood = false;
        if (assistant != null) {
            ComponentName cn = ComponentName.unflattenFromString(assistant);
            if (cn != null && cn.getPackageName().equals(getPackageName())) {
                areWeGood = true;
            }
        }
        return areWeGood;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ACTIVITY_RESUMED) setViewContent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ACTIVITY_RESUMED = true;
    }
}