package com.drhowdydoo.layoutinspector.adapter;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.os.Bundle;
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
import com.drhowdydoo.layoutinspector.ui.TreeLayoutManager;
import com.drhowdydoo.layoutinspector.ui.HierarchyViewHolder;
import com.drhowdydoo.layoutinspector.util.Utils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"InnerClassMayBeStatic","RawUseOfParameterized"})
public class ViewPagerAdapter extends RecyclerView.Adapter {

    private List<TreeNode> hierarchy = new ArrayList<>();
    private TreeViewAdapter treeViewAdapter;
    private ComponentTabViewholder componentTabViewholder;
    private final Context context;
    private final AssistSession assistSession;

    public ViewPagerAdapter(Context context, AssistSession assistSession) {
        this.context = context;
        this.assistSession = assistSession;
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
            AssistStructure.ViewNode viewNode = (AssistStructure.ViewNode) treeNode.getValue();
            assistSession.drawRect(Utils.viewNodeRectMap.get(viewNode));
        });
    }

    private void bindComponentTabViewHolder(ComponentTabViewholder holder) {
        // Handle component tab view holder logic here
        componentTabViewholder = holder;
    }

    public void setComponent(AssistStructure.ViewNode viewNode){

        componentTabViewholder.containerComponentInfo.setVisibility(View.VISIBLE);
        componentTabViewholder.tvSelectComponent.setVisibility(View.GONE);
        componentTabViewholder.imgComponentIcon.setBackgroundResource(DrawableManager.getDrawableForView(Utils.getLastSegmentOfClass(viewNode.getClassName())));
        componentTabViewholder.tvComponentName.setText(Utils.getLastSegmentOfClass(viewNode.getClassName()));
        String id = String.valueOf(viewNode.getIdEntry());
        if (!id.equalsIgnoreCase("null")) {
            componentTabViewholder.tvComponentId.setText(id);
        }else {
            componentTabViewholder.tvComponentId.setVisibility(View.GONE);
        }
        String packageName = viewNode.getIdPackage();
        if (packageName != null && packageName.isEmpty()) {
            componentTabViewholder.tvPackage.setVisibility(View.GONE);
        } else {
            componentTabViewholder.tvPackage.setText(packageName);

        }
        componentTabViewholder.tvWidth.setText(String.format("%s dp", Utils.pxToDp(context, viewNode.getWidth())));
        componentTabViewholder.tvHeight.setText(String.format("%s dp", Utils.pxToDp(context, viewNode.getHeight())));
        if (viewNode.getText() != null) {
            setTextAttribute(viewNode);
        }else {
            componentTabViewholder.containerTextAttribute.setVisibility(View.GONE);
        }

        componentTabViewholder.tvAlpha.setText(String.valueOf(viewNode.getAlpha()));
        componentTabViewholder.tvElevation.setText(String.format("%s dp",Utils.pxToDp(context, (int) viewNode.getElevation())));
        Bundle bundle = viewNode.getExtras();
        if (bundle != null) {
            int textAppearance = bundle.getInt("android:textAppearance");
            Log.d("TAG", "setComponent: " + textAppearance);
        }
    }

    private void setTextAttribute(AssistStructure.ViewNode viewNode){
        componentTabViewholder.tvTextSize.setText(String.format("%s sp", Utils.pxToSp(context, (int) viewNode.getTextSize())));
        componentTabViewholder.tvTextColor.setText(Utils.intToHexString(viewNode.getTextColor()));
        StringBuilder textStyles = getTextStyles(viewNode.getTextStyle());
        componentTabViewholder.tvTextStyle.setText(textStyles.toString());
    }

    @NonNull
    private static StringBuilder getTextStyles(int textStyle) {
        StringBuilder textStyles = new StringBuilder();
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_BOLD) != 0) textStyles.append("Bold").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_ITALIC) != 0) textStyles.append("Italic").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_UNDERLINE) != 0) textStyles.append("Underline").append(" ");
        if ((textStyle & AssistStructure.ViewNode.TEXT_STYLE_STRIKE_THRU) != 0) textStyles.append("StrikeThrough").append(" ");
        return textStyles;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public void setTreeExpanded(boolean expandTree) {
        if (expandTree) treeViewAdapter.expandAll();
        else treeViewAdapter.collapseAll();
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
                tvAlpha, tvElevation;
        LinearLayout containerTextAttribute,containerPackage,containerComponentInfo;
        TextView tvSelectComponent;
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
        }
    }

    public void setHierarchyTree(List<TreeNode> treeNodes) {
        hierarchy = treeNodes;
        notifyDataSetChanged();
    }

}
