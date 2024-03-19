package com.drhowdydoo.layoutinspector.service;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.adapter.ViewPagerAdapter;
import com.drhowdydoo.layoutinspector.ui.DrawableFrameLayout;
import com.drhowdydoo.layoutinspector.util.PreferenceManager;
import com.drhowdydoo.layoutinspector.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@SuppressWarnings("FieldCanBeLocal")
public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private AssistStructure assistStructure;
    private Animation slideUpAnimation, slideDownAnimation;
    private MaterialCardView mCardView,settingsCard;
    private DrawableFrameLayout mAssistantView;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private List<TreeNode> hierarchy = new ArrayList<>();
    private MaterialButton btnSettings;
    private boolean isSettingsShown = false;
    private int oldX = 0, oldY = 0;

    public AssistSession(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        getContext().setTheme(R.style.Theme_AppPeek);
        super.onCreate();
    }

    @Override
    public void onShow(@Nullable Bundle args, int showFlags) {
        Log.d(TAG, "onShow() ");
        mAssistantView.setVisibility(View.VISIBLE);
        mCardView.startAnimation(slideUpAnimation);
        super.onShow(args, showFlags);
    }

    @Override
    public void onHide() {
        Log.d(TAG, "onHide() ");
        mAssistantView.clearCanvas();
        mAssistantView.setVisibility(View.INVISIBLE);
        mCardView.startAnimation(slideDownAnimation);
        settingsCard.setVisibility(View.GONE);
        isSettingsShown = false;
        super.onHide();
    }

    @SuppressLint("InflateParams")
    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public View onCreateContentView() {
        Log.d(TAG, "onCreateContentView: ");

        LayoutInflater inflater = getLayoutInflater();
        mAssistantView = (DrawableFrameLayout) inflater.inflate(R.layout.assistant_layout, null);

        mCardView = mAssistantView.findViewById(R.id.card_bg);
        viewPager = mAssistantView.findViewById(R.id.viewPager);
        btnSettings = mAssistantView.findViewById(R.id.btnSetting);
        tabLayout = mAssistantView.findViewById(R.id.tabLayout);
        settingsCard = mAssistantView.findViewById(R.id.settings_layout);

        setUpAnimations();
        setUpViewPagerWithTab();
        setUpClickListeners();
        setUpTouchListener();
        setUpSettings();

        return mAssistantView;
    }

    private void setUpSettings() {
        Slider widthSlider = settingsCard.findViewById(R.id.widthSlider);
        MaterialSwitch switchLayoutBounds = settingsCard.findViewById(R.id.switchShowLayoutBounds);
        MaterialButton btnGreen = settingsCard.findViewById(R.id.btnGreen);
        MaterialButton btnRed = settingsCard.findViewById(R.id.btnRed);
        MaterialButton btnBlue = settingsCard.findViewById(R.id.btnBlue);
        MaterialButton btnPurple = settingsCard.findViewById(R.id.btnPurple);
        TextView tvViewTypeToShowBoundsFor = settingsCard.findViewById(R.id.tvViewTypeToShow);

        tvViewTypeToShowBoundsFor.setOnClickListener(v -> showMenu(v, R.menu.popup_menu));

        btnGreen.setOnClickListener(v -> {
            PreferenceManager.strokeColor = Color.GREEN;
            mAssistantView.updatePaintAttributes();
        });

        btnRed.setOnClickListener(v -> {
            PreferenceManager.strokeColor = Color.RED;
            mAssistantView.updatePaintAttributes();
        });

        btnBlue.setOnClickListener(v -> {
            PreferenceManager.strokeColor = Color.BLUE;
            mAssistantView.updatePaintAttributes();
        });

        btnPurple.setOnClickListener(v -> {
            PreferenceManager.strokeColor = Color.MAGENTA;
            mAssistantView.updatePaintAttributes();
        });

        widthSlider.addOnChangeListener((rangeSlider, value, fromUser) -> {
            PreferenceManager.strokeWidth = value;
            mAssistantView.updatePaintAttributes();
        });

        switchLayoutBounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.showLayoutBounds = isChecked;
            if (isChecked) showLayoutBounds();
            mAssistantView.invalidate();
        });
    }

    private void showMenu(View v, int menu){
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(menu, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            int menuId = menuItem.getItemId();
            PreferenceManager.viewTypeToShowBoundsOf = menuId == R.id.all ? -1 : menuId == R.id.visible ? View.VISIBLE : View.INVISIBLE;
            ((TextView) v).setText(menuItem.getTitle());
            showLayoutBounds();
            mAssistantView.invalidate();
            return true; // Return true if the click event is handled.
        });

        popup.setOnDismissListener(menu1 -> {
            // Respond to popup being dismissed.
        });

        // Show the popup menu.
        popup.show();
    }

    @Override
    public void onHandleAssist(@NonNull AssistState state) {
        super.onHandleAssist(state);
        Log.d("TAG", "onHandleAssist: ");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        assistStructure = state.getAssistStructure();
        mAssistantView.post(() -> {
            Utils.statusBarOffset = mAssistantView.visibleDisplayFrame.top;
            if (assistStructure == null) {
                hierarchy.clear();
            } else {
                hierarchy = Utils.displayViewHierarchy(assistStructure);
            }
            viewPagerAdapter.setHierarchyTree(hierarchy);
            if (PreferenceManager.showLayoutBounds) {
                showLayoutBounds();
            }
        });
    }

    private void setUpAnimations() {
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpViewPagerWithTab() {
        viewPagerAdapter = new ViewPagerAdapter(getContext(), this);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(position == 0 ? "Component" : "Hierarchy")).attach();
    }

    private void setUpClickListeners() {
        btnSettings.setOnClickListener(v -> {
            settingsCard.setVisibility(isSettingsShown ? View.GONE : View.VISIBLE);
            isSettingsShown = !isSettingsShown;
        });
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpTouchListener() {

        mAssistantView.setOnTouchListener(new View.OnTouchListener() {
            Stack<AssistStructure.ViewNode> viewNodeStack;
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (oldX != (int) event.getX() && oldY != (int) event.getY()) {
                    viewNodeStack = getViewNodeByCoordinates((int) event.getX(), (int) event.getY());
                    oldX = (int) event.getX();
                    oldY = (int) event.getY();
                }
                AssistStructure.ViewNode viewNode = viewNodeStack.pop();
                drawRect(Utils.viewNodeRectMap.get(viewNode));
                viewPagerAdapter.setComponent(viewNode);
                return false;
            }
        });
    }

    public static Stack<AssistStructure.ViewNode> getViewNodeByCoordinates(int x, int y) {
        Stack<AssistStructure.ViewNode> viewNodeStack = new Stack<>();
        for (Map.Entry<AssistStructure.ViewNode, Rect> entry : Utils.viewNodeRectMap.entrySet()) {
            Rect rect = entry.getValue();
            if (rect.contains(x, y) && entry.getKey().getVisibility() == View.VISIBLE) {
                Log.d(TAG, "getViewNodeByCoordinates: " + entry.getKey().getClassName());
                viewNodeStack.push(entry.getKey());
                if (entry.getKey().getChildCount() == 0) break;
            }
        }
        return viewNodeStack;
    }

    private void showLayoutBounds(){
            List<Rect> layoutBounds = new ArrayList<>();
            for (Map.Entry<AssistStructure.ViewNode, Rect> entry : Utils.viewNodeRectMap.entrySet()) {
                if (PreferenceManager.viewTypeToShowBoundsOf < 0 || entry.getKey().getVisibility() == PreferenceManager.viewTypeToShowBoundsOf) {
                    Rect rect = entry.getValue();
                    layoutBounds.add(rect);
                }
            }
            mAssistantView.updateLayoutBounds(layoutBounds);
    }

    public void drawRect(Rect rect) {
       mAssistantView.drawRect(rect);
    }

}
