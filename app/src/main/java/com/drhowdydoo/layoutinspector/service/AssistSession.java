package com.drhowdydoo.layoutinspector.service;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.LayoutInflater;
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

public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private AssistStructure assistStructure;
    private Animation slideUpAnimation;
    private MaterialCardView mCardView;
    private View mAssistantView;
    private ViewPager2 viewPager;
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            assistStructure = state.getAssistStructure();
            Log.d("TAG", "onHandleAssist: " + assistStructure);
            if (assistStructure != null) {
                hierarchy = Utils.displayViewHierarchy(assistStructure);
                Log.d("TAG", "onHandleAssist: Hierarchy " + hierarchy);
                viewPagerAdapter.setHierarchyTree(hierarchy);

            }
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
        LayoutInflater inflater = getLayoutInflater();
        mAssistantView = inflater.inflate(R.layout.assistant_layout, null);
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        mCardView = mAssistantView.findViewById(R.id.card_bg);
        viewPager = mAssistantView.findViewById(R.id.viewPager);
        btnTreeExpand = mAssistantView.findViewById(R.id.btnTreeExpand);
        TabLayout tabLayout = mAssistantView.findViewById(R.id.tabLayout);
        viewPagerAdapter = new ViewPagerAdapter(getContext());
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout,viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Component");
                showExpandAllButton(false);
            } else {
                tab.setText("Hierarchy");
                showExpandAllButton(true);
            }
        }).attach();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                showExpandAllButton(position != 0);
            }
        });

        btnTreeExpand.setOnClickListener(v -> {
            viewPagerAdapter.setTreeExpanded(!isTreeExpanded);
            isTreeExpanded = !isTreeExpanded;
            if (isTreeExpanded) {
                btnTreeExpand.setIcon(AppCompatResources.getDrawable(getContext(),R.drawable.rounded_collapse_all_24));
            }else {
                btnTreeExpand.setIcon(AppCompatResources.getDrawable(getContext(),R.drawable.rounded_expand_all_24));
            }
        });
        return mAssistantView;
    }

    private void showExpandAllButton(boolean showButton) {
        if (showButton) btnTreeExpand.setVisibility(View.VISIBLE);
        else btnTreeExpand.setVisibility(View.GONE);
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
