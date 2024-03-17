package com.drhowdydoo.layoutinspector.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewHolder;
import com.drhowdydoo.layoutinspector.R;

public class HierarchyViewHolder extends TreeViewHolder {
    TextView tvNode;
    ImageView imgArrow,imgIcon;
    public HierarchyViewHolder(@NonNull View itemView) {
        super(itemView);
        tvNode = itemView.findViewById(R.id.tvNode);
        imgArrow = itemView.findViewById(R.id.imgArrow);
        imgIcon = itemView.findViewById(R.id.imgIcon);
    }
    @Override
    public void bindTreeNode(TreeNode node) {
        super.bindTreeNode(node);
        // Here you can bind your node and check if it selected or not
        tvNode.setText(node.getValue().toString());
        imgIcon.setBackgroundResource(DrawableManager.getDrawableForView(node.getValue().toString()));
        if (node.isExpanded()) {
            imgArrow.setBackgroundResource(R.drawable.rounded_arrow_drop_down_24);
        } else imgArrow.setBackgroundResource(R.drawable.rounded_arrow_right_24);
        if (node.getChildren().isEmpty()) imgArrow.setVisibility(View.INVISIBLE);
        else imgArrow.setVisibility(View.VISIBLE);
    }
}
