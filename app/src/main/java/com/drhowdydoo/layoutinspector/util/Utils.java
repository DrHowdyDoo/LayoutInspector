package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

public class Utils {

    /**
     * A completed hierarchy snapshot. The roots are supplied to TreeViewAdapter; flatNodes
     * preserve the depth-first index used by component selection and parent/child navigation.
     */
    public static final class HierarchyResult {
        private final List<TreeNode> roots;
        private final List<TreeNode> flatNodes;

        private HierarchyResult(List<TreeNode> roots, List<TreeNode> flatNodes) {
            this.roots = roots;
            this.flatNodes = flatNodes;
        }

        public List<TreeNode> getRoots() {
            return roots;
        }

        public List<TreeNode> getFlatNodes() {
            return flatNodes;
        }

        public static HierarchyResult empty() {
            return new HierarchyResult(new ArrayList<>(), new ArrayList<>());
        }
    }

    /** Builds an independent snapshot. No shared static collections are read or modified. */
    public static HierarchyResult displayViewHierarchy(AssistStructure assistStructure,
                                                       int statusBarOffset) {
        if (assistStructure == null) return HierarchyResult.empty();

        List<TreeNode> roots = new ArrayList<>();
        List<TreeNode> flatNodes = new ArrayList<>();
        int windowNodeCount = assistStructure.getWindowNodeCount();

        for (int i = 0; i < windowNodeCount; i++) {
            throwIfCancelled();
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            AssistStructure.ViewNode rootViewNode = windowNode != null
                    ? windowNode.getRootViewNode() : null;
            if (rootViewNode == null) continue;

            TreeNode root = buildViewHierarchy(rootViewNode, 0, null,
                    0, statusBarOffset, true, flatNodes);
            if (root != null) roots.add(root);
        }

        return new HierarchyResult(roots, flatNodes);
    }

    private static TreeNode buildViewHierarchy(AssistStructure.ViewNode viewNode,
                                               int depth,
                                               TreeNode parent,
                                               int leftOffset,
                                               int topOffset,
                                               boolean parentVisible,
                                               List<TreeNode> flatNodes) {
        throwIfCancelled();

        boolean isVisible = parentVisible
                && viewNode.getVisibility() == View.VISIBLE
                && viewNode.getWidth() > 0
                && viewNode.getHeight() > 0
                && viewNode.getAlpha() > 0.05f;

        int childCount = viewNode.getChildCount();
        int scrollX = childCount > 0 ? viewNode.getScrollX() : 0;
        int scrollY = childCount > 0 ? viewNode.getScrollY() : 0;
        int left = leftOffset + viewNode.getLeft() - scrollX;
        int top = topOffset + viewNode.getTop() - scrollY;
        Rect bounds = new Rect(left, top, left + viewNode.getWidth(), top + viewNode.getHeight());

        AssistStructure.ViewNode parentViewNode = null;
        if (parent != null && parent.getValue() instanceof ViewNodeWrapper) {
            parentViewNode = ((ViewNodeWrapper) parent.getValue()).getViewNode();
        }

        int position = flatNodes.size();
        ViewNodeWrapper wrapper = new ViewNodeWrapper(
                viewNode,
                isVisible,
                bounds,
                depth,
                depth,
                position,
                parentViewNode,
                leftOffset,
                topOffset);
        TreeNode treeNode = new TreeNode(wrapper, R.layout.hierarchy_tree_node);
        if (parent != null) parent.addChild(treeNode);
        flatNodes.add(treeNode);

        for (int i = 0; i < childCount; i++) {
            throwIfCancelled();
            AssistStructure.ViewNode child = viewNode.getChildAt(i);
            if (child != null) {
                buildViewHierarchy(child, depth + 1, treeNode, left, top,
                        isVisible, flatNodes);
            }
        }

        return treeNode;
    }

    private static void throwIfCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Hierarchy build was superseded");
        }
    }

    public static String getLastSegmentOfClass(String className) {
        if (className == null || className.isEmpty()) return "";
        int separator = className.lastIndexOf('.');
        return separator >= 0 ? className.substring(separator + 1) : className;
    }

    public static int pxToDp(Context context, float px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / displayMetrics.density);
    }

    public static int pxToSp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / displayMetrics.scaledDensity);
    }

    public static int spToPx(Context context, int sp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(sp * displayMetrics.scaledDensity);
    }

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }

    public static float dpToPxAccurate(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density * 100f) / 100f;
    }

    public static String intToHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
