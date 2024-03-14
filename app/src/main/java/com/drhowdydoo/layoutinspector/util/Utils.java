package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static StringBuilder stringBuilder = new StringBuilder();

    public static String displayViewHierarchy(AssistStructure assistStructure) {
        int windowNodeCount = assistStructure.getWindowNodeCount();
        for (int i = 0; i < windowNodeCount; i++) {
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            displayViewHierarchyRecursive(windowNode.getRootViewNode(), 0);
        }
        return stringBuilder.toString();
    }
    private static void displayViewHierarchyRecursive(AssistStructure.ViewNode viewNode, int depth) {
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indentation.append("  ");
        }
        stringBuilder.append("\n");
        stringBuilder.append(indentation).append(getLastSegmentOfClass(viewNode.getClassName()));

        if (viewNode.getChildCount() > 0) {
            for (int i = 0; i < viewNode.getChildCount(); i++) {
                displayViewHierarchyRecursive(viewNode.getChildAt(i), depth + 1);
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
