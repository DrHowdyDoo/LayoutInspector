package com.drhowdydoo.layoutinspector.ui;

import com.drhowdydoo.layoutinspector.R;

public class DrawableManager {

    public static int getDrawableForView(String viewName) {
        switch (viewName) {
            case "TextView":
                return R.drawable.text_view_icon;
            case "ImageView":
                return R.drawable.image_view_icon;
            case "ImageButton":
                return R.drawable.image_button_icon;
            case "LinearLayout":
                return R.drawable.linear_layout_icon;
            case "RelativeLayout":
                return R.drawable.relative_layout_icon;
            case "FrameLayout":
                return R.drawable.frame_layout_icon;
            case "ConstraintLayout":
                return R.drawable.constraint_layout_icon;
            case "RecyclerView":
                return R.drawable.recycler_view_icon;
            case "RadioButton":
                return R.drawable.radio_btn_icon;
            case "SearchView":
                return R.drawable.search_view_icon;
            case "ViewGroup":
                return R.drawable.viewgroup_icon;
            case "Slider":
            case "SeekBar" :
                return R.drawable.slider_icon;
            case "ScrollView":
                return R.drawable.scroll_view_icon;
            case "HorizontalScrollView":
                return R.drawable.horizontal_scroll_view_icon;
            case "NestedScrollView":
                return R.drawable.nested_scroll_view_icon;
            case "CheckBox":
            case "MaterialCheckBox":
            case "AppCompatCheckBox":
                return R.drawable.checkbox_icon;
            case "MaterialSwitch":
            case "SwitchMaterial":
            case "SwitchCompat":
            case "CompoundButton" :
            case "Switch":
                return R.drawable.switch_icon;
            default:
                return R.drawable.view_icon;
        }
    }

}
