package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.View;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.model.Arrow;
import com.drhowdydoo.layoutinspector.model.ArrowSet;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static final Map<ViewNodeWrapper, Rect> viewNodeRectMap = new LinkedHashMap<>();
    private static final List<TreeNode> componentTree = new ArrayList<>();
    public static int statusBarOffset = 0;
    private static int index;

    public static List<TreeNode> displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        componentTree.clear();
        viewNodeRectMap.clear();
        index = 0;
        for (int i = 0; i < windowNodeCount; i++) {
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            displayViewHierarchyRecursive(windowNode.getRootViewNode(), 0, null, 0, statusBarOffset, true);
        }
        return componentTree;
    }

    private static void displayViewHierarchyRecursive(AssistStructure.ViewNode viewNode,
                                                      int depth,
                                                      TreeNode parent,
                                                      int leftOffset,
                                                      int topOffset,
                                                      boolean isVisible) {

        isVisible = isVisible
                && (viewNode.getVisibility() == View.VISIBLE)
                && (viewNode.getWidth() > 0)
                && (viewNode.getHeight() > 0)
                && (viewNode.getAlpha() > 0.05f);

        ViewNodeWrapper viewNodeWrapper = new ViewNodeWrapper(viewNode, isVisible);

        TreeNode root = new TreeNode(viewNodeWrapper, R.layout.hierarchy_tree_node);
        if (parent != null) {
            parent.addChild(root);
        }
        viewNodeWrapper.setDepth(depth);
        viewNodeWrapper.setLevelInParent(root.getLevel());
        viewNodeWrapper.setPositionInHierarchy(index);
        componentTree.add(root);
        index++;

        // Calculate absolute position
        int left = leftOffset + viewNode.getLeft()  - viewNode.getScrollX();
        int top = topOffset + viewNode.getTop() - viewNode.getScrollY();
        int right = left + viewNode.getWidth();
        int bottom = top + viewNode.getHeight();
        Rect rect = new Rect(left, top , right, bottom);
        Matrix transformation = viewNode.getTransformation();
        if (transformation != null) {
            transformation.mapRect(new RectF(rect));
        }

        if (parent != null) {
            ViewNodeWrapper parentViewNodeWrapper = (ViewNodeWrapper) parent.getValue();
            AssistStructure.ViewNode parentViewNode = parentViewNodeWrapper.getViewNode();
            ArrowSet arrowSet = new ArrowSet(new Arrow(left, (top + bottom) / 2,  leftOffset - parentViewNode.getScrollX(), (top + bottom) / 2),
                    new Arrow(left + viewNode.getWidth(), (top + bottom) / 2, parentViewNode.getWidth() + leftOffset - parentViewNode.getScrollX(), (top + bottom) / 2),
                    new Arrow((left + right) / 2, top, (left + right) / 2, topOffset - parentViewNode.getScrollY()),
                    new Arrow((left + right) / 2, top + viewNode.getHeight(), (left + right) / 2, parentViewNode.getHeight() + topOffset - parentViewNode.getScrollY()));

            viewNodeWrapper.setArrowSet(arrowSet);
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

    public static int pxToDp(Context context, float px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / displayMetrics.density);
    }

    public static int pxToSp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float scaledDensity = displayMetrics.scaledDensity;
        return Math.round(px / scaledDensity);
    }

    public static int spToPx(Context context, int sp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float scaledDensity = displayMetrics.scaledDensity;
        return Math.round(sp * scaledDensity);
    }

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }

    public static String intToHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

}
