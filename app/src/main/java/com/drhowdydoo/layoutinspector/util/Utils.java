package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static final Map<AssistStructure.ViewNode, Rect> viewNodeRectMap = new HashMap<>();
    private static final List<TreeNode> componentTree = new ArrayList<>();
    public static int statusBarOffset;

    public static List<TreeNode> displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        componentTree.clear();
        viewNodeRectMap.clear();
        for (int i = 0; i < windowNodeCount; i++) {
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            displayViewHierarchyRecursive(windowNode.getRootViewNode(), 0, null, 0, -statusBarOffset);
        }
        return componentTree;
    }

    private static void displayViewHierarchyRecursive(AssistStructure.ViewNode viewNode, int depth, TreeNode parent, int leftOffset, int topOffset) {

        TreeNode root = new TreeNode(viewNode, R.layout.hierarchy_tree_node);
        if (parent != null) {
            parent.addChild(root);
        }
        componentTree.add(root);

        // Calculate absolute position
        int left = leftOffset + viewNode.getLeft();
        int top = topOffset + viewNode.getTop();
        viewNodeRectMap.put(viewNode, new Rect(left, top, left + viewNode.getWidth(), top + viewNode.getHeight()));

        // Traverse children
        if (viewNode.getChildCount() > 0) {
            for (int i = 0; i < viewNode.getChildCount(); i++) {
                displayViewHierarchyRecursive(viewNode.getChildAt(i), depth + 1, root, left, top);
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

    public static String intToHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

}
