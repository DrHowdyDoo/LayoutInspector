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
        switch (node.getValue().toString()){
            case "TextView" : imgIcon.setBackgroundResource(R.drawable.text_view_icon);
            break;
            case "ImageView" : imgIcon.setBackgroundResource(R.drawable.image_view_icon);
            break;
            case "ImageButton" : imgIcon.setBackgroundResource(R.drawable.image_button_icon);
            break;
            case "LinearLayout" : imgIcon.setBackgroundResource(R.drawable.linear_layout_icon);
            break;
            case "RelativeLayout":imgIcon.setBackgroundResource(R.drawable.relative_layout_icon);
            break;
            case "FrameLayout": imgIcon.setBackgroundResource(R.drawable.frame_layout_icon);
            break;
            case "ConstraintLayout" : imgIcon.setBackgroundResource(R.drawable.constraint_layout_icon);
            break;
            case "RecyclerView" : imgIcon.setBackgroundResource(R.drawable.recycler_view_icon);
            break;
            case "RadioButton" : imgIcon.setBackgroundResource(R.drawable.radio_btn_icon);
            break;
            case "SearchView" : imgIcon.setBackgroundResource(R.drawable.search_view_icon);
            break;
            case "MaterialSwitch" :
            case "SwitchMaterial":
            case "SwitchCompat":
            case "Switch" : imgIcon.setBackgroundResource(R.drawable.switch_icon);
            break;
            case "ViewGroup" : imgIcon.setBackgroundResource(R.drawable.viewgroup_icon);
            break;
            case "Slider" : imgIcon.setBackgroundResource(R.drawable.slider_icon);
            break;
            case "ScrollView" : imgIcon.setBackgroundResource(R.drawable.scroll_view_icon);
            break;
            case "HorizontalScrollView" : imgIcon.setBackgroundResource(R.drawable.horizontal_scroll_view_icon);
            break;
            case "NestedScrollView" : imgIcon.setBackgroundResource(R.drawable.nested_scroll_view_icon);
            break;
            case "MaterialCheckBox" :
            case "AppCompatCheckBox" :
            case "CheckBox" : imgIcon.setBackgroundResource(R.drawable.checkbox_icon);
            break;



            default: imgIcon.setBackgroundResource(R.drawable.view_icon);

        }
        if (node.isExpanded()) {
            imgArrow.setBackgroundResource(R.drawable.rounded_arrow_drop_down_24);
        } else imgArrow.setBackgroundResource(R.drawable.rounded_arrow_right_24);
        if (node.getChildren().isEmpty()) imgArrow.setVisibility(View.INVISIBLE);
        else imgArrow.setVisibility(View.VISIBLE);
    }
}
