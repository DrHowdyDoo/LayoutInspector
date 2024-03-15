package com.drhowdydoo.layoutinspector.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewAdapter;
import com.amrdeveloper.treeview.TreeViewHolderFactory;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.service.AssistSession;
import com.drhowdydoo.layoutinspector.ui.HierarchyViewHolder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"InnerClassMayBeStatic","RawUseOfParameterized"})
public class ViewPagerAdapter extends RecyclerView.Adapter {

    private List<TreeNode> hierarchy = new ArrayList<>();
    private Context context;

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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HierarchyTabViewHolder) {
            TreeViewHolderFactory factory = (v, layout) -> new HierarchyViewHolder(v);
            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(factory);
            Log.d("TAG", "onBindViewHolder: Hierarchy " + hierarchy);
            ((HierarchyTabViewHolder) holder).recyclerView.setHasFixedSize(true);
            ((HierarchyTabViewHolder) holder).recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if (hierarchy != null) treeViewAdapter.setTreeNodes(hierarchy);
            ((HierarchyTabViewHolder) holder).recyclerView.setAdapter(treeViewAdapter);
            Log.d("TAG", "onBindViewHolder: TreeViewNodes : " +treeViewAdapter.getItemCount());

        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public class HierarchyTabViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        public HierarchyTabViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }

    public class ComponentTabViewholder extends RecyclerView.ViewHolder {
        public ComponentTabViewholder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public void setHierarchyTree(List<TreeNode> treeNodes) {
        hierarchy = treeNodes;
    }

}
