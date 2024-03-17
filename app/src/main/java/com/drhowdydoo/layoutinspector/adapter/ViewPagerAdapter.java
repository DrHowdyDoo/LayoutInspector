package com.drhowdydoo.layoutinspector.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewAdapter;
import com.amrdeveloper.treeview.TreeViewHolderFactory;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.ui.FlexibleLayoutManager;
import com.drhowdydoo.layoutinspector.ui.HierarchyViewHolder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"InnerClassMayBeStatic","RawUseOfParameterized"})
public class ViewPagerAdapter extends RecyclerView.Adapter {

    private List<TreeNode> hierarchy = new ArrayList<>();
    private TreeViewAdapter treeViewAdapter;
    private final Context context;

    public ViewPagerAdapter(Context context) {
        this.context = context;
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_view,parent,false);
            return new ComponentTabViewholder(view);
        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hierarchy_view,parent,false);
            return new HierarchyTabViewHolder(view);

        }
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HierarchyTabViewHolder) {
            TreeViewHolderFactory factory = (v, layout) -> new HierarchyViewHolder(v);
            treeViewAdapter = new TreeViewAdapter(factory);
            ((HierarchyTabViewHolder) holder).recyclerView.setHasFixedSize(true);
            FlexibleLayoutManager flexibleLayoutManager = new FlexibleLayoutManager(context);
            ((HierarchyTabViewHolder) holder).recyclerView.setLayoutManager(flexibleLayoutManager);
            if (hierarchy != null) {
                treeViewAdapter.setTreeNodes(hierarchy);
                treeViewAdapter.expandAll();
                if (hierarchy.isEmpty()) {
                    ((HierarchyTabViewHolder) holder).tvEmptyTree.setVisibility(View.VISIBLE);
                    ((HierarchyTabViewHolder) holder).recyclerView.setVisibility(View.GONE);
                }else {
                    ((HierarchyTabViewHolder) holder).tvEmptyTree.setVisibility(View.INVISIBLE);
                    ((HierarchyTabViewHolder) holder).recyclerView.setVisibility(View.VISIBLE);
                }
            }
            ((HierarchyTabViewHolder) holder).recyclerView.setAdapter(treeViewAdapter);
            ((HierarchyTabViewHolder) holder).recyclerView.setItemAnimator(null);
        }
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
        public ComponentTabViewholder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public void setHierarchyTree(List<TreeNode> treeNodes) {
        hierarchy = treeNodes;
        notifyDataSetChanged();
    }

}
