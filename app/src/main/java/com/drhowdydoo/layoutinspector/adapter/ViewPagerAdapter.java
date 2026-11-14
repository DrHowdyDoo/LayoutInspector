package com.drhowdydoo.layoutinspector.adapter;

import static android.view.View.VISIBLE;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PointF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewAdapter;
import com.amrdeveloper.treeview.TreeViewHolderFactory;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.service.AssistSession;
import com.drhowdydoo.layoutinspector.ui.DrawableManager;
import com.drhowdydoo.layoutinspector.ui.HierarchyViewHolder;
import com.drhowdydoo.layoutinspector.ui.TreeLayoutManager;
import com.drhowdydoo.layoutinspector.util.Utils;
import com.drhowdydoo.layoutinspector.util.DistanceArrowDrawer;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"InnerClassMayBeStatic","RawUseOfParameterized"})
public class ViewPagerAdapter extends RecyclerView.Adapter {

    private List<TreeNode> hierarchyRoots = new ArrayList<>();
    private List<TreeNode> flatHierarchy = new ArrayList<>();
    private boolean treeDataInitialized;
    private final java.util.Map<String, String> themeNameCache = new java.util.HashMap<>();
    private TreeViewAdapter treeViewAdapter;
    private ComponentTabViewholder componentTabViewholder;
    private final Context context;
    private final AssistSession assistSession;
    private PackageManager pm;
    private int selectedNodePosition = -1;
    private RecyclerView hierarchyRecyclerView;
    private int measurementUnitType = 0;
    private boolean hasSelectableComponents;
    private boolean componentSelected;
    private boolean isProtectedScreen;

    private LinearLayout hierarchyEmptyView;
    private TextView hierarchyEmptyTitle;
    private TextView hierarchyEmptySubtitle;

    public ViewPagerAdapter(Context context, AssistSession assistSession) {
        this.context = context;
        this.assistSession = assistSession;
        pm = context.getPackageManager();
    }



    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = viewType == 0 ? R.layout.component_view : R.layout.hierarchy_view;
        View view = inflater.inflate(layoutId, parent, false);

        if (viewType == 0) {
            return new ComponentTabViewholder(view);
        } else {
            return new HierarchyTabViewHolder(view);
        }
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HierarchyTabViewHolder) {
            bindHierarchyTabViewHolder((HierarchyTabViewHolder) holder);
        } else if (holder instanceof ComponentTabViewholder) {
            bindComponentTabViewHolder((ComponentTabViewholder) holder);
        }
    }

    private void bindHierarchyTabViewHolder(HierarchyTabViewHolder holder) {
        hierarchyRecyclerView = holder.recyclerView;
        hierarchyEmptyView = holder.emptyComponentTree;
        hierarchyEmptyTitle = holder.tvProtectedTitle;
        hierarchyEmptySubtitle = holder.tvProtectedSubtitle;

        if (treeViewAdapter == null) {
            TreeViewHolderFactory factory = (v, layout) -> new HierarchyViewHolder(v);
            treeViewAdapter = new TreeViewAdapter(factory);
            treeViewAdapter.setTreeNodeClickListener((treeNode, view) -> {
                ViewNodeWrapper wrapper = (ViewNodeWrapper) treeNode.getValue();
                if (wrapper == null || wrapper.getVisibleBounds().isEmpty()) return;

                updateSelectedTreeNode(treeNode, wrapper.getPositionInHierarchy());
                setComponent(wrapper);
            });
        }

        if (holder.recyclerView.getLayoutManager() == null) {
            holder.recyclerView.setHasFixedSize(true);
            holder.recyclerView.setLayoutManager(new TreeLayoutManager(context));
        }
        if (holder.recyclerView.getAdapter() != treeViewAdapter) {
            holder.recyclerView.setAdapter(treeViewAdapter);
            holder.recyclerView.setItemAnimator(null);
        }

        if (!treeDataInitialized) {
            treeViewAdapter.setTreeNodes(hierarchyRoots);
            treeViewAdapter.collapseAll();
            treeDataInitialized = true;
        }
        updateHierarchyEmptyState();
    }

    private void updateHierarchyEmptyState() {
        if (hierarchyEmptyView == null || hierarchyRecyclerView == null) return;

        if (isProtectedScreen) {
            // Same UI as Component tab: protected screen
            hierarchyEmptyTitle.setText(R.string.protected_view);
            hierarchyEmptyTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.baseline_shield_24, 0, 0);

            hierarchyEmptySubtitle.setText(R.string.protected_view_msg);

            hierarchyEmptyView.setVisibility(VISIBLE);
            hierarchyRecyclerView.setVisibility(View.GONE);

        } else if (!hasSelectableComponents) {
            // Same UI as Component tab: no inspectable nodes
            hierarchyEmptyTitle.setText(R.string.no_inspectable_nodes_title);
            hierarchyEmptyTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.round_hide_source_24, 0, 0);

            hierarchyEmptySubtitle.setText(
                    R.string.no_inspectable_nodes_description);

            hierarchyEmptyView.setVisibility(VISIBLE);
            hierarchyRecyclerView.setVisibility(View.GONE);

        } else {
            // Normal hierarchy available
            hierarchyEmptyView.setVisibility(View.GONE);
            hierarchyRecyclerView.setVisibility(VISIBLE);
        }
    }

    private void updateComponentEmptyState(
            boolean isProtectedScreen,
            boolean hasInspectableNodes) {

        this.isProtectedScreen = isProtectedScreen;
        this.hasSelectableComponents = hasInspectableNodes;

        if (componentSelected) {
            updateComponentPlaceholderState();
            return;
        }

        if (isProtectedScreen) {
            showProtectedScreenMessage();
        } else if (!hasInspectableNodes) {
            showNoInspectableNodesMessage();
        } else {
            hideEmptyStateMessage();
        }
    }
    private void showProtectedScreenMessage() {
        if (componentTabViewholder == null) return;

        componentTabViewholder.tvProtectedTitle.setText(R.string.protected_view);
        componentTabViewholder.tvProtectedTitle.setCompoundDrawablesWithIntrinsicBounds(
                0, R.drawable.baseline_shield_24, 0, 0);

        componentTabViewholder.tvProtectedSubtitle.setText(
                R.string.protected_view_msg);

        componentTabViewholder.emptyComponentTree.setVisibility(VISIBLE);
        componentTabViewholder.tvSelectComponent.setVisibility(View.GONE);
    }

    private void showNoInspectableNodesMessage() {
        if (componentTabViewholder == null) return;

        componentTabViewholder.tvProtectedTitle.setText(
                R.string.no_inspectable_nodes_title);

        componentTabViewholder.tvProtectedTitle.setCompoundDrawablesWithIntrinsicBounds(
                0, R.drawable.round_hide_source_24, 0, 0);

        componentTabViewholder.tvProtectedSubtitle.setText(
                R.string.no_inspectable_nodes_description);

        componentTabViewholder.emptyComponentTree.setVisibility(VISIBLE);
        componentTabViewholder.tvSelectComponent.setVisibility(View.GONE);
    }

    private void hideEmptyStateMessage() {
        if (componentTabViewholder == null) return;

        componentTabViewholder.emptyComponentTree.setVisibility(View.GONE);
        updateComponentPlaceholderState();
    }

    private void updateSelectedTreeNode(TreeNode selectedNode, int positionInHierarchy) {
        if (selectedNodePosition == positionInHierarchy) return;

        int oldVisiblePosition = -1;
        if (selectedNodePosition >= 0 && selectedNodePosition < flatHierarchy.size()) {
            TreeNode oldNode = flatHierarchy.get(selectedNodePosition);
            oldVisiblePosition = findVisiblePosition(hierarchyRoots, oldNode);
            oldNode.setSelected(false);
        }

        if (selectedNode != null) selectedNode.setSelected(true);
        selectedNodePosition = positionInHierarchy;

        if (treeViewAdapter != null) {
            // RecyclerView positions are positions in the currently visible tree, not the
            // stable depth-first indexes stored in ViewNodeWrapper.
            if (oldVisiblePosition >= 0) {
                treeViewAdapter.notifyItemChanged(oldVisiblePosition);
            }
            int newVisiblePosition = findVisiblePosition(hierarchyRoots, selectedNode);
            if (newVisiblePosition >= 0) {
                treeViewAdapter.notifyItemChanged(newVisiblePosition);
            }
        }
    }

    private void bindComponentTabViewHolder(ComponentTabViewholder holder) {
        componentTabViewholder = holder;
        updateComponentPlaceholderState();

        // ViewPager2 may create this page after a hierarchy selection was made from the
        // assistant overlay. Restore the already-selected component into the new holder.
        if (AssistSession.selectedViewNode != null) {
            setComponent(AssistSession.selectedViewNode);
            handleHierarchyNavigation(
                    AssistSession.selectedViewNode.getPositionInHierarchy(),
                    AssistSession.selectedViewNode.getViewNode().getChildCount());
        }

        componentTabViewholder.btnMoveLeft.setOnClickListener(v -> {
            if (AssistSession.selectedViewNode == null || componentTabViewholder == null) return;
            int selectedPosition = AssistSession.selectedViewNode.getPositionInHierarchy();
            if (selectedPosition < 0 || selectedPosition >= flatHierarchy.size()) return;
            TreeNode parentNode = flatHierarchy.get(selectedPosition).getParent();
            // A window root has no parent. Do not push it onto the back stack or leave
            // the navigation button in a misleading enabled state.
            if (parentNode == null) {
                handleHierarchyNavigation(selectedPosition,
                        AssistSession.selectedViewNode.getViewNode().getChildCount());
                return;
            }
            AssistSession.viewNodeWrapperStack.push(AssistSession.selectedViewNode);
            ViewNodeWrapper parentNodeWrapper = (ViewNodeWrapper) parentNode.getValue();
            showInHierarchyTab(parentNodeWrapper);
            setComponent(parentNodeWrapper);
            handleHierarchyNavigation(AssistSession.selectedViewNode.getPositionInHierarchy(),
                    parentNodeWrapper.getViewNode().getChildCount());
        });

        componentTabViewholder.btnMoveRight.setOnClickListener(v -> {
            if (AssistSession.selectedViewNode == null || componentTabViewholder == null) return;
            int position = AssistSession.selectedViewNode.getPositionInHierarchy();
            if (position < 0 || position >= flatHierarchy.size()) return;
            LinkedList<TreeNode> children = flatHierarchy.get(position).getChildren();
            if (children == null || children.isEmpty()) return;
            TreeNode childNode = children.get(0);
            ViewNodeWrapper childNodeWrapper;
            if (!AssistSession.viewNodeWrapperStack.isEmpty()) {
                childNodeWrapper = AssistSession.viewNodeWrapperStack.pop();
            }else {
                childNodeWrapper = childNode != null ? (ViewNodeWrapper) childNode.getValue() : null;
            }
            if (childNodeWrapper == null) return;
            showInHierarchyTab(childNodeWrapper);
            setComponent(childNodeWrapper);
            int childCount = childNodeWrapper.getViewNode().getChildCount();
            handleHierarchyNavigation(AssistSession.selectedViewNode.getPositionInHierarchy(), childCount);
        });
    }


    private void updateComponentPlaceholderState() {
        if (componentTabViewholder == null) return;

        if (componentSelected) {
            componentTabViewholder.containerComponentInfo.setVisibility(VISIBLE);
            componentTabViewholder.tvSelectComponent.setVisibility(View.GONE);
            componentTabViewholder.emptyComponentTree.setVisibility(View.GONE);
            return;
        }

        componentTabViewholder.containerComponentInfo.setVisibility(View.GONE);

        if (isProtectedScreen) {
            showProtectedScreenMessage();
        } else if (!hasSelectableComponents) {
            showNoInspectableNodesMessage();
        } else {
            componentTabViewholder.emptyComponentTree.setVisibility(View.GONE);
            componentTabViewholder.tvSelectComponent.setVisibility(VISIBLE);
        }
    }

    public void handleHierarchyNavigation(int currentPos, int childCount){
        if (componentTabViewholder == null) return;
        boolean hasParent = currentPos >= 0 && currentPos < flatHierarchy.size()
                && flatHierarchy.get(currentPos).getParent() != null;
        componentTabViewholder.btnMoveLeft.setVisibility(hasParent ? VISIBLE : View.INVISIBLE);
        componentTabViewholder.btnMoveRight.setVisibility(childCount > 0 ? VISIBLE : View.INVISIBLE);
    }

    public void resetComponentView(){
        componentSelected = false;
        if (componentTabViewholder == null) return;
        componentTabViewholder.btnMoveLeft.setVisibility(View.INVISIBLE);
        componentTabViewholder.btnMoveRight.setVisibility(View.INVISIBLE);
        updateComponentPlaceholderState();
    }

    public void smoothScrollToCenter(RecyclerView recyclerView, int position) {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;

        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) lm;

        int first = layoutManager.findFirstCompletelyVisibleItemPosition();
        int last = layoutManager.findLastCompletelyVisibleItemPosition();

        // If item is already fully visible, don’t scroll
        if (position >= first && position <= last) {
            return;
        }

        RecyclerView.SmoothScroller smoothScroller = getSmoothScroller(recyclerView, position, layoutManager);
        recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
    }

    private static RecyclerView.SmoothScroller getSmoothScroller(RecyclerView recyclerView, int position, LinearLayoutManager layoutManager) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_ANY; // not start or end, we'll handle offset manually
            }

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            public int calculateDtToFit(int viewStart, int viewEnd,
                                           int boxStart, int boxEnd,
                                           int snapPreference) {
                int viewCenter = (viewStart + viewEnd) / 2;
                int boxCenter = (boxStart + boxEnd) / 2;
                return boxCenter - viewCenter; // shift so item lands in center
            }
        };

        smoothScroller.setTargetPosition(position);
        return smoothScroller;
    }


    public void showInHierarchyTab(ViewNodeWrapper viewNodeWrapper) {
        if (viewNodeWrapper == null
                || viewNodeWrapper.getVisibleBounds().isEmpty() || flatHierarchy.isEmpty()) return;
        int index = viewNodeWrapper.getPositionInHierarchy();
        if (index < 0 || index >= flatHierarchy.size()) return;

        TreeNode selectedNode = flatHierarchy.get(index);
        updateSelectedTreeNode(selectedNode, index);

        // Only scroll when the node is currently visible. A node under a collapsed ancestor
        // remains selected and will render selected when the user expands that branch.
        int visiblePosition = findVisiblePosition(hierarchyRoots, selectedNode);
        if (visiblePosition >= 0) {
            smoothScrollToCenter(hierarchyRecyclerView, visiblePosition);
        }
    }

    private int findVisiblePosition(List<TreeNode> roots, TreeNode target) {
        int[] visibleIndex = {0};
        for (TreeNode root : roots) {
            int result = findVisiblePosition(root, target, visibleIndex);
            if (result >= 0) return result;
        }
        return -1;
    }

    private int findVisiblePosition(TreeNode node, TreeNode target, int[] visibleIndex) {
        int currentPosition = visibleIndex[0]++;
        if (node == target) return currentPosition;
        if (!node.isExpanded()) return -1;

        LinkedList<TreeNode> children = node.getChildren();
        if (children == null) return -1;
        for (TreeNode child : children) {
            int result = findVisiblePosition(child, target, visibleIndex);
            if (result >= 0) return result;
        }
        return -1;
    }


    public void setComponent(ViewNodeWrapper viewNodeWrapper){
        if (viewNodeWrapper == null || viewNodeWrapper.getViewNode() == null) {
            return;
        }
        // Occlusion is a foreground-layer heuristic, not a reason to make a node impossible
        // to inspect. Keep any node with real visible geometry selectable from the hierarchy.
        if (viewNodeWrapper.getVisibleBounds().isEmpty()) {
            assistSession.drawRect(null);
            DistanceArrowDrawer.clearCanvas();
            AssistSession.selectedViewNode = null;
            selectedNodePosition = -1;
            componentSelected = false;
            resetComponentView();
            return;
        }
        assistSession.drawRect(viewNodeWrapper.getVisibleBounds());
        assistSession.drawArrow(viewNodeWrapper);
        AssistSession.selectedViewNode = viewNodeWrapper;
        selectedNodePosition = viewNodeWrapper.getPositionInHierarchy();
        componentSelected = true;
        AssistStructure.ViewNode viewNode = viewNodeWrapper.getViewNode();
        if (componentTabViewholder == null) return;

        updateComponentPlaceholderState();
        componentTabViewholder.imgComponentIcon.setBackgroundResource(DrawableManager.getDrawableForView(viewNodeWrapper.getSimpleClassName()));
        componentTabViewholder.tvComponentName.setText(viewNodeWrapper.getSimpleClassName());
        String id = String.valueOf(viewNode.getIdEntry());
        if (!id.equalsIgnoreCase("null")) {
            componentTabViewholder.tvComponentId.setVisibility(VISIBLE);
            componentTabViewholder.tvComponentId.setText(String.format("id: %s",id));
        }else {
            componentTabViewholder.tvComponentId.setVisibility(View.INVISIBLE);
        }

        String packageName = viewNode.getIdPackage();
        if (packageName == null || packageName.isEmpty()) {
            componentTabViewholder.containerPackage.setVisibility(View.GONE);
            componentTabViewholder.containerTheme.setVisibility(View.GONE);
        } else {
            componentTabViewholder.containerPackage.setVisibility(VISIBLE);
            componentTabViewholder.tvPackage.setText(packageName);
            setThemeAttributes(packageName, viewNode.getId());

        }
        componentTabViewholder.tvWidth.setText(getMeasurement(viewNode.getWidth()));
        componentTabViewholder.tvHeight.setText(getMeasurement(viewNode.getHeight()));
        if (viewNode.getText() != null) {
            setTextAttribute(viewNode);
            componentTabViewholder.containerTextAttribute.setVisibility(VISIBLE);
        }else {
            componentTabViewholder.containerTextAttribute.setVisibility(View.GONE);
        }

        componentTabViewholder.tvAlpha.setText(String.valueOf(viewNode.getAlpha()));
        componentTabViewholder.tvElevation.setText(getMeasurement(viewNode.getElevation()));
        String visibility = viewNode.getVisibility() == VISIBLE ? "Visible" : "Invisible";
        componentTabViewholder.tvVisibility.setText(visibility);
        String effectiveVisibility = viewNodeWrapper.isVisible()
                ? "Visible"
                : "Invisible";

        componentTabViewholder.tvEffectiveVisibility.setText(effectiveVisibility);
        CharSequence contentDescription = viewNode.getContentDescription();
        if (contentDescription == null || contentDescription.toString().isEmpty()) {
            componentTabViewholder.containerContentDesc.setVisibility(View.GONE);
        }
        else {
            componentTabViewholder.containerContentDesc.setVisibility(VISIBLE);
            componentTabViewholder.tvContentDesc.setText(contentDescription);
        }
        componentTabViewholder.tvChildCount.setText(String.valueOf(viewNode.getChildCount()));

    }

    private String getMeasurement(float measurement){
        if (measurementUnitType == 0) {
            return String.format("%s dp", Utils.pxToDp(context, measurement));
        } else {
            return String.format("%s px", (int) measurement);
        }
    }

    private void setThemeAttributes(String packageName, int id) {
        String cachedThemeName = themeNameCache.get(packageName);
        if (cachedThemeName != null) {
            applyThemeName(cachedThemeName);
            return;
        }

        try {
            Resources resources = pm.getResourcesForApplication(packageName);
            int theme = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA).theme;
            String themeName = theme != 0 ? resources.getResourceEntryName(theme) : "";
            themeNameCache.put(packageName, themeName);
            applyThemeName(themeName);
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            themeNameCache.put(packageName, "");
            componentTabViewholder.containerTheme.setVisibility(View.GONE);
            Log.d("TAG", "Theme resource unavailable for package: " + packageName);
        }
    }

    private void applyThemeName(String themeName) {
        boolean hasTheme = themeName != null && !themeName.isEmpty();
        componentTabViewholder.containerTheme.setVisibility(hasTheme ? VISIBLE : View.GONE);
        if (hasTheme) componentTabViewholder.tvTheme.setText(themeName);
    }

    private void setTextAttribute(AssistStructure.ViewNode viewNode){
        componentTabViewholder.tvTextSize.setText(String.format("%s sp", Utils.pxToSp(context, (int) viewNode.getTextSize())));
        componentTabViewholder.tvTextColor.setText(Utils.intToHexString(viewNode.getTextColor()));
        String textStyles = getTextStyles(viewNode.getTextStyle());
        componentTabViewholder.tvTextStyle.setText(textStyles);
    }

    @NonNull
    private static String getTextStyles(int textStyle) {
        StringBuilder textStyles = new StringBuilder();
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_BOLD) != 0) textStyles.append("Bold").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_ITALIC) != 0) textStyles.append("Italic").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_UNDERLINE) != 0) textStyles.append("Underline").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_STRIKE_THRU) != 0) textStyles.append("StrikeThrough").append(" ");
        return textStyles.toString().isEmpty() ? "Normal" : textStyles.toString();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public void showFunctionalityUnavailableIndicator() {
        updateComponentPlaceholderState();
        updateHierarchyEmptyState();
    }

    public void clearSelectedViewNodePosition() {
        if (selectedNodePosition >= 0 && selectedNodePosition < flatHierarchy.size()) {
            flatHierarchy.get(selectedNodePosition).setSelected(false);
        }
        selectedNodePosition = -1;
        resetComponentView();
    }

    public void setMeasurementUnitType(int measurementUnitType) {
        this.measurementUnitType = measurementUnitType;
        notifyMeasurementUnitChange();
    }

    private void notifyMeasurementUnitChange() {
        if (AssistSession.selectedViewNode == null || componentTabViewholder == null) return;
        componentTabViewholder.tvWidth.setText(getMeasurement(AssistSession.selectedViewNode.getViewNode().getWidth()));
        componentTabViewholder.tvHeight.setText(getMeasurement(AssistSession.selectedViewNode.getViewNode().getHeight()));
        componentTabViewholder.tvElevation.setText(getMeasurement(AssistSession.selectedViewNode.getViewNode().getElevation()));
    }

    public class HierarchyTabViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        LinearLayout emptyComponentTree;
        TextView tvProtectedTitle;
        TextView tvProtectedSubtitle;
        public HierarchyTabViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
            emptyComponentTree = itemView.findViewById(R.id.emptyComponentTree);
            tvProtectedTitle = itemView.findViewById(R.id.tv_protected_title);
            tvProtectedSubtitle = itemView.findViewById(R.id.tv_protected_subtitle);
        }
    }

    public class ComponentTabViewholder extends RecyclerView.ViewHolder {
        ImageView imgComponentIcon;
        TextView tvPackage,tvWidth, tvHeight, tvComponentId,
                tvComponentName,tvTextSize,tvTextColor,tvTextStyle,
                tvAlpha, tvElevation,tvVisibility,tvEffectiveVisibility,
                tvContentDesc,tvTheme,tvChildCount;
        LinearLayout containerTextAttribute,containerPackage,containerComponentInfo,containerContentDesc,containerTheme, emptyComponentTree;
        TextView tvSelectComponent,tvProtectedTitle,tvProtectedSubtitle;
        MaterialButton btnMoveLeft, btnMoveRight;
        public ComponentTabViewholder(@NonNull View itemView) {
            super(itemView);
            tvSelectComponent = itemView.findViewById(R.id.tvSelectComponent);
            containerComponentInfo = itemView.findViewById(R.id.containerComponentInfo);
            imgComponentIcon = itemView.findViewById(R.id.imgComponentIcon);
            tvComponentName = itemView.findViewById(R.id.tvComponentName);
            tvComponentId = itemView.findViewById(R.id.tvComponentId);
            tvPackage = itemView.findViewById(R.id.tvPackageValue);
            tvWidth = itemView.findViewById(R.id.tvWidth);
            tvHeight = itemView.findViewById(R.id.tvHeight);
            tvTextSize = itemView.findViewById(R.id.tvTextSize);
            tvTextColor = itemView.findViewById(R.id.tvTextColor);
            tvTextStyle = itemView.findViewById(R.id.tvTextStyle);
            containerTextAttribute = itemView.findViewById(R.id.containerTextAttribute);
            containerPackage = itemView.findViewById(R.id.containerPackage);
            tvAlpha = itemView.findViewById(R.id.tvAlpha);
            tvElevation = itemView.findViewById(R.id.tvElevation);
            tvVisibility = itemView.findViewById(R.id.tvVisibility);
            tvEffectiveVisibility = itemView.findViewById(R.id.tvEffectiveVisibility);
            tvContentDesc = itemView.findViewById(R.id.tvContentDesc);
            containerContentDesc = itemView.findViewById(R.id.containerContentDesc);
            containerTheme = itemView.findViewById(R.id.containerTheme);
            tvTheme = itemView.findViewById(R.id.tvTheme);
            btnMoveLeft = itemView.findViewById(R.id.btnMoveLeft);
            btnMoveRight = itemView.findViewById(R.id.btnMoveRight);
            tvChildCount = itemView.findViewById(R.id.tvChildCount);
            emptyComponentTree = itemView.findViewById(R.id.emptyComponentTree);
            tvProtectedTitle = itemView.findViewById(R.id.tv_protected_title);
            tvProtectedSubtitle = itemView.findViewById(R.id.tv_protected_subtitle);
        }
    }

    private boolean containsSelectableForegroundNode() {
        for (TreeNode node : flatHierarchy) {
            if (node == null || !(node.getValue() instanceof ViewNodeWrapper)) continue;
            ViewNodeWrapper wrapper = (ViewNodeWrapper) node.getValue();
            if (wrapper.getDepth() > 0
                    && wrapper.isVisible()
                    && !wrapper.getVisibleBounds().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void setHierarchyTree(
            List<TreeNode> roots,
            List<TreeNode> allNodes,
            boolean hasAssistNodeData) {

        hierarchyRoots = roots != null ? roots : new ArrayList<>();
        flatHierarchy = allNodes != null ? allNodes : new ArrayList<>();

        selectedNodePosition = -1;
        componentSelected = false;
        treeDataInitialized = false;

        // A root node alone does not mean that the foreground screen contains
        // anything the user can select. This also filters incidental launcher
        // nodes when an app such as Google Discover is in the foreground.
        hasSelectableComponents =
                hasAssistNodeData && containsSelectableForegroundNode();

        // AssistStructure does not expose a reliable "secure window" flag here.
        // Preserve the existing fallback: no root data and no hierarchy is
        // presented using the protected-view empty state.
        isProtectedScreen = !hasAssistNodeData && flatHierarchy.isEmpty();

        updateComponentEmptyState(isProtectedScreen, hasSelectableComponents);

        if (treeViewAdapter != null) {
            treeViewAdapter.setTreeNodes(hierarchyRoots);
            treeViewAdapter.collapseAll();
            treeDataInitialized = true;
        }

        updateHierarchyEmptyState();
        updateComponentPlaceholderState();
    }

}
