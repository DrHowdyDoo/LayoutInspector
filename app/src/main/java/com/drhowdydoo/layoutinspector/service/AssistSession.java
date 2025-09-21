package com.drhowdydoo.layoutinspector.service;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.adapter.ViewPagerAdapter;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;
import com.drhowdydoo.layoutinspector.ui.DrawableFrameLayout;
import com.drhowdydoo.layoutinspector.util.DistanceArrowDrawer;
import com.drhowdydoo.layoutinspector.util.DrawableLayoutHelper;
import com.drhowdydoo.layoutinspector.util.LayoutBoundsDrawer;
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

@SuppressWarnings("FieldCanBeLocal")
public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private AssistStructure assistStructure;
    private Animation slideUpAnimation, slideDownAnimation;
    private MaterialCardView mCardView;
    private FrameLayout settingsCard;
    private DrawableFrameLayout mAssistantView;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private List<TreeNode> hierarchy = new ArrayList<>();
    private MaterialButton btnSettings,btnExpandCollapse;
    private boolean isSettingsShown = false;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int oldX = 0, oldY = 0;
    private boolean isExpanded = true;
    private Transition changeBoundTransition;
    private Transition autoTransition, slideAnimation;
    public List<ViewNodeWrapper> viewNodes;
    public int viewNodePointer = 0;
    private static ViewNodeWrapper closestChild = null;
    private CoordinatorLayout draggableContainer;

    public AssistSession(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        getContext().setTheme(R.style.Theme_AppPeek);
        preferences = getContext().getSharedPreferences("com.drhowdydoo.layoutinspector.preferences", MODE_PRIVATE);
        editor = preferences.edit();
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
        DrawableLayoutHelper.clearCanvas();
        viewPagerAdapter.resetComponentView();
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
        btnExpandCollapse = mAssistantView.findViewById(R.id.btnExpandCollapse);
        draggableContainer = mAssistantView.findViewById(R.id.draggable_container);

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
        MaterialSwitch switchShowViewPosition = settingsCard.findViewById(R.id.switchShowViewPosition);
        MaterialButton btnGreen = settingsCard.findViewById(R.id.btnGreen);
        MaterialButton btnRed = settingsCard.findViewById(R.id.btnRed);
        MaterialButton btnBlue = settingsCard.findViewById(R.id.btnBlue);
        MaterialButton btnPurple = settingsCard.findViewById(R.id.btnPurple);
        TextView tvViewTypeToShowBoundsFor = settingsCard.findViewById(R.id.tvViewTypeToShow);
        View settingsOverlay = settingsCard.findViewById(R.id.settings_overlay);

        tvViewTypeToShowBoundsFor.setOnClickListener(v -> showMenu(v, R.menu.popup_menu));

        widthSlider.setValue(preferences.getFloat("SETTINGS_STROKE_WIDTH",2.5f));
        switchLayoutBounds.setChecked(preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS",false));
        switchShowViewPosition.setChecked(preferences.getBoolean("SETTINGS_SHOW_VIEW_POSITION", false));
        int viewType = preferences.getInt("SETTINGS_VIEW_TYPE",View.VISIBLE);
        tvViewTypeToShowBoundsFor.setText(viewType < 0 ? "All" : viewType == View.VISIBLE ? "Visible views" : "Invisible views");

        btnGreen.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.GREEN).apply();
            mAssistantView.notifyPaintChange();
        });

        btnRed.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.RED).apply();
            mAssistantView.notifyPaintChange();
        });

        btnBlue.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.BLUE).apply();
            mAssistantView.notifyPaintChange();
        });

        btnPurple.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.MAGENTA).apply();
            mAssistantView.notifyPaintChange();
        });

        widthSlider.addOnChangeListener((rangeSlider, value, fromUser) -> {
            editor.putFloat("SETTINGS_STROKE_WIDTH",value).apply();
            mAssistantView.notifyPaintChange();
        });

        switchLayoutBounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", isChecked).apply();
            if (isChecked) showLayoutBounds();
            mAssistantView.notifyPreferenceChange();
        });

        switchShowViewPosition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("SETTINGS_SHOW_VIEW_POSITION", isChecked).apply();
            mAssistantView.notifyPreferenceChange();
        });

        settingsOverlay.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) settingsCard.getParent(), new Slide(Gravity.BOTTOM));
            settingsCard.setVisibility(View.GONE);
            isSettingsShown = false;
        });
    }

    private void showMenu(View v, int menu){
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(menu, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            int menuId = menuItem.getItemId();
            int viewType = menuId == R.id.all ? -1 : menuId == R.id.visible ? View.VISIBLE : View.INVISIBLE;
            editor.putInt("SETTINGS_VIEW_TYPE",viewType).apply();
            ((TextView) v).setText(menuItem.getTitle());
            showLayoutBounds();
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

        assistStructure = state.getAssistStructure();
        mAssistantView.post(() -> {
            WindowInsets insets = mAssistantView.getRootWindowInsets();
            if (insets.getSystemWindowInsetTop() == 0) Utils.statusBarOffset = -mAssistantView.visibleDisplayFrame.top;
            if (assistStructure == null) {
                hierarchy.clear();
            } else {
                hierarchy = Utils.displayViewHierarchy(assistStructure);
            }
            viewPagerAdapter.setHierarchyTree(hierarchy);
            if (preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", false)) {
                showLayoutBounds();
            }
        });
    }

    private void setUpAnimations() {
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        changeBoundTransition = new ChangeBounds();
        autoTransition = new AutoTransition();
        slideAnimation = new Slide();
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpViewPagerWithTab() {
        viewPagerAdapter = new ViewPagerAdapter(getContext(), this);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(position == 0 ? "Component" : "Hierarchy")).attach();
    }

    private void setUpClickListeners() {
        btnSettings.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) settingsCard.getParent(), new Slide(Gravity.BOTTOM));
            settingsCard.setVisibility(isSettingsShown ? View.GONE : View.VISIBLE);
            isSettingsShown = !isSettingsShown;
        });



        btnExpandCollapse.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(settingsCard, autoTransition);
            if (isExpanded) {
                collapseCardView();
                btnExpandCollapse.setIconResource(R.drawable.rounded_keyboard_arrow_up_24);
            } else {
                expandCardView();
                btnExpandCollapse.setIconResource(R.drawable.rounded_keyboard_arrow_down_24);
            }
            isExpanded = !isExpanded;
        });
    }

    private void expandCardView() {
        int expandedHeight = Utils.dpToPx(getContext(), 312);
        ViewGroup.LayoutParams layoutParams = mCardView.getLayoutParams();

        float cardTop = mCardView.getY();

        if (cardTop <= expandedHeight) {
            // not enough space above → expand downwards
            mCardView.setY(expandedHeight);
        }
        layoutParams.height = expandedHeight;
        TransitionManager.beginDelayedTransition(mCardView, changeBoundTransition);
        mCardView.setLayoutParams(layoutParams);
    }

    private void collapseCardView() {
        TransitionManager.beginDelayedTransition(mCardView, changeBoundTransition);
        ViewGroup.LayoutParams layoutParams = mCardView.getLayoutParams();
        layoutParams.height = Utils.dpToPx(getContext(), 59);
        mCardView.setLayoutParams(layoutParams);
    }


    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpTouchListener() {

        mAssistantView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (oldX != (int) event.getX() && oldY != (int) event.getY()) {
                    viewNodes = getViewNodeByCoordinates((int) event.getX(), (int) event.getY());
                    viewNodePointer = closestChild != null ? viewNodes.indexOf(closestChild) : 0;
                    viewPagerAdapter.handlePointerBounds(viewNodePointer, viewNodes.size() - 1);
                    oldX = (int) event.getX();
                    oldY = (int) event.getY();
                }
                if (viewNodes == null || viewNodes.isEmpty()) return false;
                ViewNodeWrapper viewNodeWrapper = viewNodes.get(viewNodePointer);
                DistanceArrowDrawer.setArrowSet(viewNodeWrapper.getArrowSet());
                drawRect(Utils.viewNodeRectMap.get(viewNodeWrapper));
                viewPagerAdapter.setComponent(viewNodeWrapper);
                return false;
            }
        });

        mCardView.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Clamp X and Y to screen bounds
                        newX = Math.max(0, Math.min(newX, draggableContainer.getWidth() - view.getWidth()));
                        newY = Math.max(0, Math.min(newY, draggableContainer.getHeight() - view.getHeight()));

                        view.setX(newX);
                        view.setY(newY);
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    public static List<ViewNodeWrapper> getViewNodeByCoordinates(int x, int y) {
        List<ViewNodeWrapper> viewNodeStack = new ArrayList<>();
        float shortestDistance = Float.MAX_VALUE;
        for (Map.Entry<ViewNodeWrapper, Rect> entry : Utils.viewNodeRectMap.entrySet()) {
            Rect rect = entry.getValue();
            if (rect.contains(x, y)) {
                if (!entry.getKey().isVisible() || Utils.getLastSegmentOfClass(entry.getKey().getViewNode().getClassName()).equalsIgnoreCase("view")) {
                    continue;
                }
                viewNodeStack.add(entry.getKey());
                float distance = (float) Math.hypot(rect.centerX() - x, rect.centerY() - y);
                if (distance <= shortestDistance) {
                    shortestDistance = distance;
                    closestChild = entry.getKey();
                }
            }
        }
        return viewNodeStack;
    }

    private void showLayoutBounds(){
            List<Rect> layoutBounds = new ArrayList<>();
            int viewType = preferences.getInt("SETTINGS_VIEW_TYPE",View.VISIBLE);
            for (Map.Entry<ViewNodeWrapper, Rect> entry : Utils.viewNodeRectMap.entrySet()) {
                if (viewType < 0 || entry.getKey().showViewNode(viewType)) {
                    Rect rect = entry.getValue();
                    layoutBounds.add(rect);
                }
            }
            LayoutBoundsDrawer.updateLayoutBounds(layoutBounds);
            mAssistantView.notifyPreferenceChange();
    }

    public void drawRect(Rect rect) {
       mAssistantView.drawRect(rect);
    }

    public void drawArrow() {
        DistanceArrowDrawer.setArrowSet(viewNodes.get(viewNodePointer).getArrowSet());
    }


}
