package com.drhowdydoo.layoutinspector.ui;

import static com.drhowdydoo.layoutinspector.util.Utils.getLastSegmentOfClass;

import android.app.assist.AssistStructure;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amrdeveloper.treeview.TreeNode;
import com.amrdeveloper.treeview.TreeViewHolder;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

public class HierarchyViewHolder extends TreeViewHolder {
    TextView tvNode;
    ImageView imgArrow,imgIcon;
    LinearLayout nodeContainer;
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
        ViewNodeWrapper viewNodeWrapper = (ViewNodeWrapper) node.getValue();
        AssistStructure.ViewNode viewNode = viewNodeWrapper.getViewNode();

        String viewName = getLastSegmentOfClass(viewNode.getClassName());
        String viewId = viewNode.getIdEntry() != null ? viewNode.getIdEntry() : "";

        SpannableStringBuilder nodeText = new SpannableStringBuilder();
        SpannableString boldSpan = new SpannableString(viewName);
        boldSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, viewName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        nodeText.append(boldSpan);
        if (!viewId.isEmpty()) nodeText.append(" : ");
        SpannableString normalSpan = new SpannableString(viewId);
        normalSpan.setSpan(new StyleSpan(Typeface.NORMAL), 0, viewId.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        nodeText.append(normalSpan);

        tvNode.setText(nodeText);
        imgIcon.setBackgroundResource(DrawableManager.getDrawableForView(getLastSegmentOfClass(viewNode.getClassName())));
        imgArrow.setBackgroundResource(node.isExpanded() ? R.drawable.rounded_arrow_drop_down_24 : R.drawable.rounded_arrow_right_24);
        imgArrow.setVisibility(viewNode.getChildCount() == 0 ? View.INVISIBLE : View.VISIBLE);
        nodeContainer.setSelected(node.isSelected());
    }
}
