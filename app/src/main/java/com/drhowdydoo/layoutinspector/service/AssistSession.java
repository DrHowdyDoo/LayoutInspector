package com.drhowdydoo.layoutinspector.service;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.viewpager2.widget.ViewPager2;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.adapter.ViewPagerAdapter;
import com.drhowdydoo.layoutinspector.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private AssistStructure assistStructure;
    private Animation slideUpAnimation, slideDownAnimation;
    private MaterialCardView mCardView;
    private View mAssistantView;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private List<TreeNode> hierarchy = new ArrayList<>();
    private MaterialButton btnTreeExpand;
    private boolean isTreeExpanded = true;

    public AssistSession(Context context) {
        super(context);
    }


    @Override
    public void onHandleAssist(@NonNull AssistState state) {
        super.onHandleAssist(state);
        Log.d("TAG", "onHandleAssist: ");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            assistStructure = state.getAssistStructure();
            if (assistStructure != null) {
                hierarchy = Utils.displayViewHierarchy(assistStructure);
            } else {
                hierarchy.clear();
            }
            viewPagerAdapter.setHierarchyTree(hierarchy);
        }
    }

    @Override
    public void onCreate() {
        getContext().setTheme(R.style.Theme_AppPeek);
        super.onCreate();
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public View onCreateContentView() {
        Log.d("TAG", "onCreateContentView: ");
        LayoutInflater inflater = getLayoutInflater();
        mAssistantView = inflater.inflate(R.layout.assistant_layout, null);
        mCardView = mAssistantView.findViewById(R.id.card_bg);
        viewPager = mAssistantView.findViewById(R.id.viewPager);
        btnTreeExpand = mAssistantView.findViewById(R.id.btnTreeExpand);
        tabLayout = mAssistantView.findViewById(R.id.tabLayout);
        setUpAnimations();
        setUpViewPagerWithTab();
        setUpClickListeners();
        setUpTouchListener();

        return mAssistantView;
    }

    private void setUpTouchListener() {

    }

    private void showExpandAllButton(boolean showButton) {
        Log.d(TAG, "showExpandAllButton: " + showButton);
        showButton = showButton && assistStructure != null;
        if (showButton) btnTreeExpand.setVisibility(View.VISIBLE);
        else btnTreeExpand.setVisibility(View.GONE);
    }

    private void setUpAnimations() {
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpViewPagerWithTab() {
        viewPagerAdapter = new ViewPagerAdapter(getContext());
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Component");
            } else {
                tab.setText("Hierarchy");
            }
        }).attach();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                showExpandAllButton(position != 0);
            }
        });
    }

    private void setUpClickListeners() {
        btnTreeExpand.setOnClickListener(v -> {
            viewPagerAdapter.setTreeExpanded(!isTreeExpanded);
            isTreeExpanded = !isTreeExpanded;
            if (isTreeExpanded) {
                btnTreeExpand.setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.rounded_collapse_all_24));
            } else {
                btnTreeExpand.setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.rounded_expand_all_24));
            }
        });
    }

    @Override
    public void onShow(@Nullable Bundle args, int showFlags) {
        Log.d(TAG, "onShow: ");
        mAssistantView.setVisibility(View.VISIBLE);
        mCardView.startAnimation(slideUpAnimation);
        super.onShow(args, showFlags);
    }

    @Override
    public void onHide() {
        Log.d(TAG, "onHide: ");
        mAssistantView.setVisibility(View.INVISIBLE);
        mCardView.startAnimation(slideDownAnimation);
        super.onHide();
    }
}
