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
    private static final List<TreeNode> componentTree = new ArrayList<>();
    public static final Map<AssistStructure.ViewNode, Rect> viewNodeRectMap = new HashMap<>();

    public static List<TreeNode> displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        componentTree.clear();
        viewNodeRectMap.clear();
        for (int i = 0; i < windowNodeCount; i++) {
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            displayViewHierarchyRecursive(windowNode.getRootViewNode(), 0,null);
        }
        return componentTree;
    }
    private static void displayViewHierarchyRecursive(AssistStructure.ViewNode viewNode, int depth, TreeNode parent) {

        TreeNode root = new TreeNode(getLastSegmentOfClass(viewNode.getClassName()),R.layout.hierarchy_tree_node);
        if (parent != null) parent.addChild(root);
        componentTree.add(root);
        viewNodeRectMap.put(viewNode,new Rect(viewNode.getLeft(),viewNode.getTop(),viewNode.getLeft() + viewNode.getWidth(), viewNode.getTop() + viewNode.getHeight()));

        if (viewNode.getChildCount() > 0) {
            for (int i = 0; i < viewNode.getChildCount(); i++) {
                displayViewHierarchyRecursive(viewNode.getChildAt(i), depth + 1,root);
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
