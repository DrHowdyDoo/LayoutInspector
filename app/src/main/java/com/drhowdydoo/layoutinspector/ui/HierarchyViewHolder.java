package com.drhowdydoo.layoutinspector.ui;

import static com.drhowdydoo.layoutinspector.util.Utils.getLastSegmentOfClass;

import android.app.assist.AssistStructure;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewHolder;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

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
        ViewNodeWrapper viewNodeWrapper = (ViewNodeWrapper) node.getValue();
        AssistStructure.ViewNode viewNode = viewNodeWrapper.getViewNode();
        tvNode.setText(getLastSegmentOfClass(viewNode.getClassName()));
        imgIcon.setBackgroundResource(DrawableManager.getDrawableForView(getLastSegmentOfClass(viewNode.getClassName())));
        imgArrow.setBackgroundResource(node.isExpanded() ? R.drawable.rounded_arrow_drop_down_24 : R.drawable.rounded_arrow_right_24);
        imgArrow.setVisibility(viewNode.getChildCount() == 0 ? View.INVISIBLE : View.VISIBLE);
    }
}
