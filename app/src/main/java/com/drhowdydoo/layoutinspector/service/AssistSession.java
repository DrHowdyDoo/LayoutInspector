package com.drhowdydoo.layoutinspector.service;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.view.ViewStub;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("FieldCanBeLocal")
public class AssistSession extends VoiceInteractionSession {

    private static final String TAG = "AssistSession";
    private Animation slideUpAnimation, slideDownAnimation;
    private MaterialCardView mCardView;
    private FrameLayout settingsCard;
    private ViewStub settingsStub;
    private DrawableFrameLayout mAssistantView;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    // flatHierarchy is depth-first and is used for stable node indexes/navigation.
    // hierarchyRoots contains only actual window roots and is passed to TreeViewAdapter.
    private List<TreeNode> flatHierarchy = new ArrayList<>();
    private List<TreeNode> hierarchyRoots = new ArrayList<>();
    private MaterialButton btnSettings,btnExpandCollapse;
    private boolean isSettingsShown = false;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private boolean isExpanded = true;
    private Transition changeBoundTransition;
    public static ViewNodeWrapper selectedViewNode = null;
    public static Stack<ViewNodeWrapper> viewNodeWrapperStack = new Stack<>();
    private CoordinatorLayout draggableContainer;

    private static final Set<String> VIEWGROUP_CLASSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "android.widget.FrameLayout",
                    "android.widget.LinearLayout",
                    "android.widget.RelativeLayout",
                    "androidx.constraintlayout.widget.ConstraintLayout",
                    "android.widget.ScrollView",
                    "android.widget.GridLayout",
                    "androidx.coordinatorlayout.widget.CoordinatorLayout",
                    "androidx.recyclerview.widget.RecyclerView",
                    "android.view.ViewGroup"
            ))
    );

    private final ExecutorService hierarchyExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger hierarchyGeneration = new AtomicInteger();
    private volatile Future<?> hierarchyTask;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AssistSession(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        getContext().setTheme(R.style.Theme_AppPeek);
        preferences = getContext().getSharedPreferences("com.drhowdydoo.layoutinspector.preferences", MODE_PRIVATE);
        editor = preferences.edit();
        super.onCreate();
        Log.d("AssistantDebug", "AssistSession: onCreate()");
    }

    @Override
    public void onShow(@Nullable Bundle args, int showFlags) {
        Log.d("AssistantDebug", "AssistSession: onShow()");
        if (mAssistantView != null) mAssistantView.setVisibility(View.VISIBLE);
        if (mCardView != null && slideUpAnimation != null) {
            mCardView.startAnimation(slideUpAnimation);
        }
        super.onShow(args, showFlags);
    }

    @Override
    public void onHide() {
        Log.d(TAG, "onHide() ");
        DrawableLayoutHelper.clearCanvas();
        if (viewPagerAdapter != null) viewPagerAdapter.resetComponentView();
        if (mAssistantView != null) mAssistantView.setVisibility(View.INVISIBLE);
        if (mCardView != null && slideDownAnimation != null) {
            mCardView.startAnimation(slideDownAnimation);
        }
        if (settingsCard != null) settingsCard.setVisibility(View.GONE);
        isSettingsShown = false;
        super.onHide();
    }

    @SuppressLint("InflateParams")
    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public View onCreateContentView() {
        Log.d("AssistantDebug", "AssistSession: onCreateContentView()");

        LayoutInflater inflater = getLayoutInflater();
        mAssistantView = (DrawableFrameLayout) inflater.inflate(R.layout.assistant_layout, null);

        mCardView = mAssistantView.findViewById(R.id.card_bg);
        viewPager = mAssistantView.findViewById(R.id.viewPager);
        btnSettings = mAssistantView.findViewById(R.id.btnSetting);
        tabLayout = mAssistantView.findViewById(R.id.tabLayout);
        settingsStub = mAssistantView.findViewById(R.id.settings_stub);
        btnExpandCollapse = mAssistantView.findViewById(R.id.btnExpandCollapse);
        draggableContainer = mAssistantView.findViewById(R.id.draggable_container);

        setUpAnimations();
        setUpViewPagerWithTab();
        setUpClickListeners();
        setUpTouchListener();

        return mAssistantView;
    }


    private void inflateSettingsIfNeeded() {
        if (settingsCard != null) return;
        if (settingsStub == null) return;
        settingsCard = (FrameLayout) settingsStub.inflate();
        settingsCard.setVisibility(View.GONE);
        setUpSettings();
    }

    private void setUpSettings() {
        Slider widthSlider = settingsCard.findViewById(R.id.widthSlider);
        MaterialSwitch switchLayoutBounds = settingsCard.findViewById(R.id.switchShowLayoutBounds);
        MaterialSwitch switchShowViewPosition = settingsCard.findViewById(R.id.switchShowViewPosition);
        MaterialButton btnGreen = settingsCard.findViewById(R.id.btnGreen);
        MaterialButton btnRed = settingsCard.findViewById(R.id.btnRed);
        MaterialButton btnBlue = settingsCard.findViewById(R.id.btnBlue);
        MaterialButton btnPurple = settingsCard.findViewById(R.id.btnPurple);
        MaterialButton btnPink = settingsCard.findViewById(R.id.btnPink);
        TextView tvViewTypeToShowBoundsFor = settingsCard.findViewById(R.id.tvViewTypeToShow);
        View settingsOverlay = settingsCard.findViewById(R.id.settings_overlay);
        MaterialButton unitsDpBtn = settingsCard.findViewById(R.id.unit_dp_btn);
        MaterialButton unitsPxBtn = settingsCard.findViewById(R.id.unit_px_btn);
        TextView tvMeasurementUnitLabel = settingsCard.findViewById(R.id.tvMeasurementUnitLabel);

        tvViewTypeToShowBoundsFor.setOnClickListener(v -> showMenu(v, R.menu.popup_menu));

        widthSlider.setValue(preferences.getFloat("SETTINGS_STROKE_WIDTH",2.5f));
        switchLayoutBounds.setChecked(preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS",false));
        switchShowViewPosition.setChecked(preferences.getBoolean("SETTINGS_SHOW_VIEW_POSITION", false));
        int viewType = preferences.getInt("SETTINGS_VIEW_TYPE",View.VISIBLE);
        tvViewTypeToShowBoundsFor.setText(viewType < 0 ? "All" : viewType == View.VISIBLE ? "Visible views" : "Invisible views");

        btnGreen.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.parseColor("#2E7D32")).apply();
            mAssistantView.notifyPaintChange();
        });

        btnRed.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.parseColor("#C62828")).apply();
            mAssistantView.notifyPaintChange();
        });

        btnBlue.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.parseColor("#1565C0")).apply();
            mAssistantView.notifyPaintChange();
        });

        btnPurple.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.parseColor("#FF8F00")).apply();
            mAssistantView.notifyPaintChange();
        });

        btnPink.setOnClickListener(v -> {
            editor.putInt("SETTINGS_STROKE_COLOR",Color.parseColor("#FF4081")).apply();
            mAssistantView.notifyPaintChange();
        });


        widthSlider.addOnChangeListener((rangeSlider, value, fromUser) -> {
            editor.putFloat("SETTINGS_STROKE_WIDTH",value).apply();
            mAssistantView.notifyPaintChange();
        });

        switchLayoutBounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", isChecked).apply();
            if (isChecked) {
                showLayoutBounds();
            } else {
                // Release the previous hierarchy snapshot held by the overlay drawer.
                LayoutBoundsDrawer.updateLayoutBounds(new ArrayList<>());
            }
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

        int measurementUnit = preferences.getInt("APP_SETTINGS_UNIT_TYPE",0);
        Log.d(TAG, "setUpSettings: " + measurementUnit);
        if (measurementUnit == 0) {
            unitsDpBtn.setChecked(true);
            unitsPxBtn.setChecked(false);
        } else {
            unitsDpBtn.setChecked(false);
            unitsPxBtn.setChecked(true);
        }

        unitsDpBtn.setOnClickListener(v -> {
            editor.putInt("APP_SETTINGS_UNIT_TYPE",0).apply();
            viewPagerAdapter.setMeasurementUnitType(0);
            unitsPxBtn.setChecked(false);
            mAssistantView.notifyPreferenceChange();
        });

        unitsPxBtn.setOnClickListener(v -> {
            editor.putInt("APP_SETTINGS_UNIT_TYPE",1).apply();
            viewPagerAdapter.setMeasurementUnitType(1);
            unitsDpBtn.setChecked(false);
            mAssistantView.notifyPreferenceChange();
        });

        tvMeasurementUnitLabel.setText(String.format("%s (1 dp ~ %s px)",   tvMeasurementUnitLabel.getText(), Utils.dpToPxAccurate(getContext(), 1f)));

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

        final AssistStructure structure = state.getAssistStructure();
        final int generation = hierarchyGeneration.incrementAndGet();

        // Do not spend CPU building a hierarchy which has already been replaced by a newer request.
        Future<?> previousTask = hierarchyTask;
        if (previousTask != null) previousTask.cancel(true);

        int statusBarOffset = 0;
        if (mAssistantView != null) {
            WindowInsets insets = mAssistantView.getRootWindowInsets();
            if (insets != null && insets.getSystemWindowInsetTop() == 0) {
                statusBarOffset = -mAssistantView.visibleDisplayFrame.top;
            }
        }
        final int capturedStatusBarOffset = statusBarOffset;

        selectedViewNode = null;
        viewNodeWrapperStack.clear();
        if (viewPagerAdapter != null) viewPagerAdapter.clearSelectedViewNodePosition();

        hierarchyTask = hierarchyExecutor.submit(() -> {
            final Utils.HierarchyResult result;
            try {
                result = structure == null
                        ? Utils.HierarchyResult.empty()
                        : Utils.displayViewHierarchy(structure, capturedStatusBarOffset);
            } catch (CancellationException ignored) {
                return;
            }

            if (Thread.currentThread().isInterrupted()
                    || generation != hierarchyGeneration.get()) {
                return;
            }

            mainHandler.post(() -> {
                // A slower, older assist request must never overwrite a newer hierarchy.
                if (generation != hierarchyGeneration.get() || viewPagerAdapter == null) return;

                hierarchyRoots = result.getRoots();
                flatHierarchy = result.getFlatNodes();
                viewPagerAdapter.setHierarchyTree(hierarchyRoots, flatHierarchy);

                if (preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", false)) {
                    showLayoutBounds();
                } else {
                    LayoutBoundsDrawer.updateLayoutBounds(new ArrayList<>());
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        hierarchyGeneration.incrementAndGet();
        Future<?> task = hierarchyTask;
        if (task != null) task.cancel(true);
        hierarchyExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        selectedViewNode = null;
        viewNodeWrapperStack.clear();
        flatHierarchy = new ArrayList<>();
        hierarchyRoots = new ArrayList<>();
        DrawableLayoutHelper.clearCanvas();
        super.onDestroy();
    }

    private void setUpAnimations() {
        slideUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        changeBoundTransition = new ChangeBounds();
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setUpViewPagerWithTab() {
        viewPagerAdapter = new ViewPagerAdapter(getContext(), this);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(position == 0 ? "Component" : "Hierarchy")).attach();
    }

    private void setUpClickListeners() {
        btnSettings.setOnClickListener(v -> {
            inflateSettingsIfNeeded();
            if (settingsCard == null) return;
            TransitionManager.beginDelayedTransition((ViewGroup) settingsCard.getParent(),
                    new Slide(Gravity.BOTTOM));
            settingsCard.setVisibility(isSettingsShown ? View.GONE : View.VISIBLE);
            isSettingsShown = !isSettingsShown;
        });



        btnExpandCollapse.setOnClickListener(v -> {
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
        mAssistantView.setOnTouchListener((view, event) -> {
            // Hit-testing the complete hierarchy is O(number of views). Do it once per tap,
            // never for every ACTION_MOVE event delivered during a gesture.
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;

            if (flatHierarchy.isEmpty()) {
                if (viewPagerAdapter != null) {
                    viewPagerAdapter.showFunctionalityUnavailableIndicator();
                }
                return false;
            }

            ViewNodeWrapper wrapper = getTreeNodeByCoordinates(
                    (int) event.getX(), (int) event.getY());
            if (wrapper == null) return false;

            int position = wrapper.getPositionInHierarchy();
            if (position < 0 || position >= flatHierarchy.size()) return false;
            TreeNode treeNode = flatHierarchy.get(position);
            if (treeNode == null || viewPagerAdapter == null) return false;

            viewPagerAdapter.handleHierarchyNavigation(position,
                    wrapper.getViewNode().getChildCount());
            viewPagerAdapter.showInHierarchyTab(wrapper);
            viewPagerAdapter.setComponent(wrapper);
            return false;
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
                        newX = Math.max(0, Math.min(newX,
                                draggableContainer.getWidth() - view.getWidth()));
                        newY = Math.max(0, Math.min(newY,
                                draggableContainer.getHeight() - view.getHeight()));
                        view.setX(newX);
                        view.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    /**
     * Returns the top-most selectable node using the same reverse depth-first precedence as
     * the original insertion-ordered map, without allocating a candidate list or scanning a map.
     */
    public ViewNodeWrapper getTreeNodeByCoordinates(int x, int y) {
        int viewType = preferences.getInt("SETTINGS_VIEW_TYPE", View.VISIBLE);
        ViewNodeWrapper topMostParentNode = null;

        for (int i = flatHierarchy.size() - 1; i >= 0; i--) {
            Object value = flatHierarchy.get(i).getValue();
            if (!(value instanceof ViewNodeWrapper)) continue;

            ViewNodeWrapper current = (ViewNodeWrapper) value;
            Rect bounds = current.getBounds();
            if (bounds == null || !bounds.contains(x, y)
                    || skipInvisibleViews(current, viewType)) {
                continue;
            }

            if (current.getViewNode().getChildCount() == 0 && !isScrimView(current)) {
                return current;
            }
            if (topMostParentNode == null && current.getViewNode().getChildCount() > 0) {
                topMostParentNode = current;
            }
        }
        return topMostParentNode;
    }

    private boolean skipInvisibleViews(ViewNodeWrapper node, int viewType) {
        return !node.isVisible() && viewType == View.VISIBLE;
    }

    private boolean isScrimView(ViewNodeWrapper viewNodeWrapper) {
        if (flatHierarchy.isEmpty()) return false;
        ViewNodeWrapper root = (ViewNodeWrapper) flatHierarchy.get(0).getValue();
        boolean scrimview = "View".equalsIgnoreCase(viewNodeWrapper.getSimpleClassName())
                && viewNodeWrapper.getViewNode().getWidth() == root.getViewNode().getWidth();
        boolean isViewGroup = VIEWGROUP_CLASSES.contains(
                viewNodeWrapper.getViewNode().getClassName());
        return scrimview || isViewGroup;
    }

    private void showLayoutBounds() {
        List<Rect> layoutBounds = new ArrayList<>();
        int viewType = preferences.getInt("SETTINGS_VIEW_TYPE", View.VISIBLE);
        for (TreeNode treeNode : flatHierarchy) {
            ViewNodeWrapper wrapper = (ViewNodeWrapper) treeNode.getValue();
            if (viewType < 0 || wrapper.showViewNode(viewType)) {
                layoutBounds.add(wrapper.getBounds());
            }
        }
        LayoutBoundsDrawer.updateLayoutBounds(layoutBounds);
        if (mAssistantView != null) mAssistantView.notifyPreferenceChange();
    }

    public void drawRect(Rect rect) {
       mAssistantView.drawRect(rect);
    }

    public void drawArrow(ViewNodeWrapper selectedViewNode) {
        DistanceArrowDrawer.setArrowSet(selectedViewNode.getArrowSet());
    }


}
