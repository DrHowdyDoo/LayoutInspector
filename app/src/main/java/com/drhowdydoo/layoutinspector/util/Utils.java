package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static final Map<ViewNodeWrapper, Rect> viewNodeRectMap = new HashMap<>();
    private static final List<TreeNode> componentTree = new ArrayList<>();
    public static int statusBarOffset;

    public static List<TreeNode> displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        componentTree.clear();
        viewNodeRectMap.clear();
        for (int i = 0; i < windowNodeCount; i++) {
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            displayViewHierarchyRecursive(windowNode.getRootViewNode(), 0, null, 0, -statusBarOffset, true);
        }
        return componentTree;
    }

    private static void displayViewHierarchyRecursive(AssistStructure.ViewNode viewNode,
                                                      int depth,
                                                      TreeNode parent,
                                                      int leftOffset,
                                                      int topOffset,
                                                      boolean isVisible) {

        isVisible = isVisible & (viewNode.getVisibility() == View.VISIBLE);
        ViewNodeWrapper viewNodeWrapper = new ViewNodeWrapper(viewNode, isVisible);

        TreeNode root = new TreeNode(viewNodeWrapper, R.layout.hierarchy_tree_node);
        if (parent != null) {
            parent.addChild(root);
        }
        componentTree.add(root);

        // Calculate absolute position
        int left = leftOffset + viewNode.getLeft()  - viewNode.getScrollX();
        int top = topOffset + viewNode.getTop() - viewNode.getScrollY();
        Rect rect = new Rect(left, top , left + viewNode.getWidth(), top + viewNode.getHeight());
        Matrix transformation = viewNode.getTransformation();
        if (transformation != null) {
            transformation.mapRect(new RectF(rect));
        }
        viewNodeRectMap.put(viewNodeWrapper, rect);

        // Traverse children
        if (viewNode.getChildCount() > 0) {
            for (int i = 0; i < viewNode.getChildCount(); i++) {
                displayViewHierarchyRecursive(viewNode.getChildAt(i), depth + 1, root, left, top, isVisible);
            }
        }
    }


    public static String getLastSegmentOfClass(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        String[] segments = className.split("\\.");
        return segments[segments.length - 1];
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / displayMetrics.density);
    }

    public static int pxToSp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float scaledDensity = displayMetrics.scaledDensity;
        return Math.round(px / scaledDensity);
    }

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }

    public static String intToHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

}
