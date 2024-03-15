package com.drhowdydoo.layoutinspector.service;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.adapter.ViewPagerAdapter;
import com.drhowdydoo.layoutinspector.util.Utils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private AssistStructure assistStructure;
    private Animation slideUpAnimation;
    private MaterialCardView mCardView;
    private View mAssistantView;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;

    public AssistSession(Context context) {
        super(context);
    }


    @Override
    public void onHandleAssist(@NonNull AssistState state) {
        super.onHandleAssist(state);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            assistStructure = state.getAssistStructure();
        }
        if (assistStructure != null) {
            String views = Utils.displayViewHierarchy(assistStructure);
            Log.d(TAG, "Hierarchy : " + views);
        }

    }

    @Override
    public void onCreate() {
        getContext().setTheme(R.style.Theme_AppPeek);
        super.onCreate();
    }

    @Override
    public View onCreateContentView() {
        LayoutInflater inflater = getLayoutInflater();
        mAssistantView = inflater.inflate(R.layout.assistant_layout, null);
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        mCardView = mAssistantView.findViewById(R.id.card_bg);
        viewPager = mAssistantView.findViewById(R.id.viewPager);
        TabLayout tabLayout = mAssistantView.findViewById(R.id.tabLayout);
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout,viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Component");
            } else {
                tab.setText("Hierarchy");
            }
        }).attach();
        return mAssistantView;
    }

    private void setGradient(TextView view) {
        TextPaint paint = view.getPaint();
        float width = paint.measureText((String) view.getText());

        Shader textShader = new LinearGradient(0, 0, width, 0,
                new int[]{
                        Color.parseColor("#A770EF"),
                        Color.parseColor("#CF8BF3"),
                        Color.parseColor("#FDB99B"),
                }, null, Shader.TileMode.CLAMP);
        view.getPaint().setShader(textShader);
    }

    @Override
    public void onShow(@Nullable Bundle args, int showFlags) {
        mAssistantView.setVisibility(View.VISIBLE);
        mCardView.setAnimation(slideUpAnimation);
        super.onShow(args, showFlags);
    }

    @Override
    public void onHide() {
        mAssistantView.setVisibility(View.INVISIBLE);
        super.onHide();
    }
}
