package com.drhowdydoo.layoutinspector.adapter;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"InnerClassMayBeStatic","RawUseOfParameterized"})
public class ViewPagerAdapter extends RecyclerView.Adapter {

    private List<TreeNode> hierarchy = new ArrayList<>();
    private TreeViewAdapter treeViewAdapter;
    private ComponentTabViewholder componentTabViewholder;
    private final Context context;
    private final AssistSession assistSession;
    private PackageManager pm;

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
        // Set up RecyclerView for hierarchy tab
        TreeViewHolderFactory factory = (v, layout) -> new HierarchyViewHolder(v);
        treeViewAdapter = new TreeViewAdapter(factory);
        holder.recyclerView.setHasFixedSize(true);
        TreeLayoutManager treeLayoutManager = new TreeLayoutManager(context);
        holder.recyclerView.setLayoutManager(treeLayoutManager);

        // Bind hierarchy data
        if (hierarchy != null) {
            treeViewAdapter.setTreeNodes(hierarchy);
            treeViewAdapter.expandAll();
            boolean isEmpty = hierarchy.isEmpty();
            holder.tvEmptyTree.setVisibility(isEmpty ? View.VISIBLE : View.INVISIBLE);
            holder.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        // Set up RecyclerView adapter and click listener
        holder.recyclerView.setAdapter(treeViewAdapter);
        holder.recyclerView.setItemAnimator(null);
        treeViewAdapter.setTreeNodeClickListener((treeNode, view) -> {
            ViewNodeWrapper viewNodeWrapper = (ViewNodeWrapper) treeNode.getValue();
            assistSession.drawRect(Utils.viewNodeRectMap.get(viewNodeWrapper));
            setComponent(viewNodeWrapper.getViewNode());
        });
    }

    private void bindComponentTabViewHolder(ComponentTabViewholder holder) {
        // Handle component tab view holder logic here
        componentTabViewholder = holder;

        componentTabViewholder.btnMoveLeft.setOnClickListener(v -> {
            assistSession.viewNodePointer -= 1;
            handlePointerBounds(assistSession.viewNodePointer, assistSession.viewNodes.size() - 1);
            assistSession.drawRect(Utils.viewNodeRectMap.get(assistSession.viewNodes.get(assistSession.viewNodePointer)));
            assistSession.drawArrow();
            setComponent(assistSession.viewNodes.get(assistSession.viewNodePointer).getViewNode());
        });

        componentTabViewholder.btnMoveRight.setOnClickListener(v -> {
            assistSession.viewNodePointer += 1;
            handlePointerBounds(assistSession.viewNodePointer, assistSession.viewNodes.size() - 1);
            assistSession.drawRect(Utils.viewNodeRectMap.get(assistSession.viewNodes.get(assistSession.viewNodePointer)));
            assistSession.drawArrow();
            setComponent(assistSession.viewNodes.get(assistSession.viewNodePointer).getViewNode());
        });
    }

    public void handlePointerBounds(int currentPos, int end){
        componentTabViewholder.btnMoveLeft.setVisibility(currentPos != 0 ? View.VISIBLE : View.INVISIBLE);
        componentTabViewholder.btnMoveRight.setVisibility(currentPos != end ? View.VISIBLE : View.INVISIBLE);
    }

    public void resetComponentView(){
        componentTabViewholder.containerComponentInfo.setVisibility(View.INVISIBLE);
        componentTabViewholder.tvSelectComponent.setVisibility(View.VISIBLE);
    }

    public void setComponent(AssistStructure.ViewNode viewNode){

        componentTabViewholder.containerComponentInfo.setVisibility(View.VISIBLE);
        componentTabViewholder.tvSelectComponent.setVisibility(View.GONE);
        componentTabViewholder.imgComponentIcon.setBackgroundResource(DrawableManager.getDrawableForView(Utils.getLastSegmentOfClass(viewNode.getClassName())));
        componentTabViewholder.tvComponentName.setText(Utils.getLastSegmentOfClass(viewNode.getClassName()));
        String id = String.valueOf(viewNode.getIdEntry());
        if (!id.equalsIgnoreCase("null")) {
            componentTabViewholder.tvComponentId.setVisibility(View.VISIBLE);
            componentTabViewholder.tvComponentId.setText(String.format("id: %s",id));
        }else {
            componentTabViewholder.tvComponentId.setVisibility(View.GONE);
        }

        String packageName = viewNode.getIdPackage();
        if (packageName == null || packageName.isEmpty()) {
            componentTabViewholder.containerPackage.setVisibility(View.GONE);
        } else {
            componentTabViewholder.containerPackage.setVisibility(View.VISIBLE);
            componentTabViewholder.tvPackage.setText(packageName);
            setThemeAttributes(packageName, viewNode.getId());

        }
        componentTabViewholder.tvWidth.setText(String.format("%s dp", Utils.pxToDp(context, viewNode.getWidth())));
        componentTabViewholder.tvHeight.setText(String.format("%s dp", Utils.pxToDp(context, viewNode.getHeight())));
        if (viewNode.getText() != null) {
            setTextAttribute(viewNode);
            componentTabViewholder.containerTextAttribute.setVisibility(View.VISIBLE);
        }else {
            componentTabViewholder.containerTextAttribute.setVisibility(View.GONE);
        }

        componentTabViewholder.tvAlpha.setText(String.valueOf(viewNode.getAlpha()));
        componentTabViewholder.tvElevation.setText(String.format("%s dp",Utils.pxToDp(context, (int) viewNode.getElevation())));
        String visibility = viewNode.getVisibility() == View.VISIBLE ? "Visible" : "Invisible";
        componentTabViewholder.tvVisibility.setText(visibility);
        CharSequence contentDescription = viewNode.getContentDescription();
        if (contentDescription == null || contentDescription.toString().isEmpty()) {
            componentTabViewholder.containerContentDesc.setVisibility(View.GONE);
        }
        else {
            componentTabViewholder.containerContentDesc.setVisibility(View.VISIBLE);
            componentTabViewholder.tvContentDesc.setText(contentDescription);
        }

    }

    private void setThemeAttributes(String packageName, int id) {
        try {
            Resources resources = pm.getResourcesForApplication(packageName);
            int theme = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA).theme;

            String themeName = resources.getResourceEntryName(theme);
            int themeContainerVisibility = themeName.isEmpty() ? View.GONE : View.VISIBLE;
            componentTabViewholder.tvTheme.setText(resources.getResourceEntryName(theme));
            componentTabViewholder.containerTheme.setVisibility(themeContainerVisibility);

        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            Log.e("TAG", e + " for package: " + packageName);
            componentTabViewholder.containerTheme.setVisibility(View.GONE);
        }
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

    public class HierarchyTabViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        TextView tvEmptyTree;
        public HierarchyTabViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
            tvEmptyTree = itemView.findViewById(R.id.tvEmptyTree);
        }
    }

    public class ComponentTabViewholder extends RecyclerView.ViewHolder {
        ImageView imgComponentIcon;
        TextView tvPackage,tvWidth, tvHeight, tvComponentId,
                tvComponentName,tvTextSize,tvTextColor,tvTextStyle,
                tvAlpha, tvElevation,tvVisibility,tvContentDesc,tvTheme;
        LinearLayout containerTextAttribute,containerPackage,containerComponentInfo,containerContentDesc,containerTheme;
        TextView tvSelectComponent;
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
            tvContentDesc = itemView.findViewById(R.id.tvContentDesc);
            containerContentDesc = itemView.findViewById(R.id.containerContentDesc);
            containerTheme = itemView.findViewById(R.id.containerTheme);
            tvTheme = itemView.findViewById(R.id.tvTheme);
            btnMoveLeft = itemView.findViewById(R.id.btnMoveLeft);
            btnMoveRight = itemView.findViewById(R.id.btnMoveRight);
        }
    }

    public void setHierarchyTree(List<TreeNode> treeNodes) {
        hierarchy = treeNodes;
        notifyDataSetChanged();
    }

}
