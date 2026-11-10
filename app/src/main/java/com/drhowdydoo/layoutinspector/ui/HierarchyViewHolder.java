package com.drhowdydoo.layoutinspector.ui;

import android.app.assist.AssistStructure;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewHolder;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

public class HierarchyViewHolder extends TreeViewHolder {
    private final TextView tvNode;
    private final ImageView imgArrow;
    private final ImageView imgIcon;
    private final LinearLayout nodeContainer;

    public HierarchyViewHolder(@NonNull View itemView) {
        super(itemView);
        tvNode = itemView.findViewById(R.id.tvNode);
        imgArrow = itemView.findViewById(R.id.imgArrow);
        imgIcon = itemView.findViewById(R.id.imgIcon);
        nodeContainer = itemView.findViewById(R.id.node_container);
    }

    @Override
    public void bindTreeNode(TreeNode node) {
        super.bindTreeNode(node);
        ViewNodeWrapper wrapper = (ViewNodeWrapper) node.getValue();
        AssistStructure.ViewNode viewNode = wrapper.getViewNode();
        String viewName = wrapper.getSimpleClassName();
        String viewId = viewNode.getIdEntry() != null ? viewNode.getIdEntry() : "";

        SpannableStringBuilder text = new SpannableStringBuilder(viewName);
        if (!viewName.isEmpty()) {
            text.setSpan(new StyleSpan(Typeface.BOLD), 0, viewName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!viewId.isEmpty()) text.append(" : ").append(viewId);

        tvNode.setText(text);
        imgIcon.setBackgroundResource(DrawableManager.getDrawableForView(viewName));
        imgArrow.setBackgroundResource(node.isExpanded()
                ? R.drawable.rounded_arrow_drop_down_24
                : R.drawable.rounded_arrow_right_24);
        imgArrow.setVisibility(viewNode.getChildCount() == 0 ? View.INVISIBLE : View.VISIBLE);
        nodeContainer.setSelected(node.isSelected());
    }
}
