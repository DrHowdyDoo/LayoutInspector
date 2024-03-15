package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;

import com.amrdeveloper.treeview.TreeNode;
import com.drhowdydoo.layoutinspector.R;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    private static List<TreeNode> componentTree;

    public static List<TreeNode> displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        componentTree = new ArrayList<>();
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

        if (viewNode.getChildCount() > 0) {
            for (int i = 0; i < viewNode.getChildCount(); i++) {
                displayViewHierarchyRecursive(viewNode.getChildAt(i), depth + 1,root);
            }
        }
    }

    private static String getLastSegmentOfClass(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        String[] segments = className.split("\\.");
        return segments[segments.length - 1];
    }


}
