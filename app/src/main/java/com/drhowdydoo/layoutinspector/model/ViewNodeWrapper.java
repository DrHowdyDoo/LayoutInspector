package com.drhowdydoo.layoutinspector.model;

import android.app.assist.AssistStructure;
import android.graphics.Rect;
import android.view.View;

/**
 * Data associated with one AssistStructure node.
 * Bounds are calculated once while the hierarchy is built and then treated as immutable.
 */
public class ViewNodeWrapper {

    private AssistStructure.ViewNode viewNode;
    private boolean visibility;
    private final Rect bounds;
    private int depth;
    private int levelInParent;
    private int positionInHierarchy;
    private String simpleClassName;

    // Arrow coordinates are calculated lazily, only when the user selects this node.
    private final AssistStructure.ViewNode parentViewNode;
    private final int parentLeft;
    private final int parentTop;
    private ArrowSet arrowSet;

    public ViewNodeWrapper(AssistStructure.ViewNode viewNode,
                           boolean visibility,
                           Rect bounds,
                           int depth,
                           int levelInParent,
                           int positionInHierarchy,
                           AssistStructure.ViewNode parentViewNode,
                           int parentLeft,
                           int parentTop) {
        this.viewNode = viewNode;
        this.visibility = visibility;
        this.bounds = bounds;
        this.depth = depth;
        this.levelInParent = levelInParent;
        this.positionInHierarchy = positionInHierarchy;
        this.parentViewNode = parentViewNode;
        this.parentLeft = parentLeft;
        this.parentTop = parentTop;
        this.simpleClassName = getLastSegment(viewNode != null ? viewNode.getClassName() : null);
    }

    /** Compatibility constructor retained for callers outside this source set. */
    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility) {
        this(viewNode, visibility, new Rect(), 0, 0, 0, null, 0, 0);
    }

    /** Compatibility constructor retained for callers outside this source set. */
    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility, ArrowSet arrowSet) {
        this(viewNode, visibility, new Rect(), 0, 0, 0, null, 0, 0);
        this.arrowSet = arrowSet;
    }

    public AssistStructure.ViewNode getViewNode() {
        return viewNode;
    }

    /** Retained for compatibility with the original mutable wrapper API. */
    public void setViewNode(AssistStructure.ViewNode viewNode) {
        this.viewNode = viewNode;
        this.simpleClassName = getLastSegment(viewNode != null ? viewNode.getClassName() : null);
        this.arrowSet = null;
    }

    public boolean isVisible() {
        return visibility;
    }

    /** Retained for compatibility with the original mutable wrapper API. */
    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public boolean showViewNode(int visibilityFlag) {
        return (visibilityFlag == View.VISIBLE) == visibility;
    }

    /** Returns the precomputed absolute bounds for this node. Do not mutate the returned Rect. */
    public Rect getBounds() {
        return bounds;
    }

    public ArrowSet getArrowSet() {
        if (arrowSet == null) {
            if (parentViewNode == null) {
                // Root nodes have no parent-relative distances, but the draw path expects a set.
                arrowSet = new ArrowSet();
            } else {
                int left = bounds.left;
                int top = bounds.top;
                int right = bounds.right;
                int bottom = bounds.bottom;
                int centerX = (left + right) / 2;
                int centerY = (top + bottom) / 2;
                int parentScrollX = parentViewNode.getScrollX();
                int parentScrollY = parentViewNode.getScrollY();

                arrowSet = new ArrowSet(
                        new Arrow(left, centerY, parentLeft - parentScrollX, centerY),
                        new Arrow(right, centerY,
                                parentViewNode.getWidth() + parentLeft - parentScrollX, centerY),
                        new Arrow(centerX, top, centerX, parentTop - parentScrollY),
                        new Arrow(centerX, bottom, centerX,
                                parentViewNode.getHeight() + parentTop - parentScrollY));
            }
        }
        return arrowSet;
    }

    public void setArrowSet(ArrowSet arrowSet) {
        this.arrowSet = arrowSet;
    }

    public int getDepth() {
        return depth;
    }

    /** Retained for compatibility with the original mutable wrapper API. */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getPositionInHierarchy() {
        return positionInHierarchy;
    }

    /** Retained for compatibility with the original mutable wrapper API. */
    public void setPositionInHierarchy(int positionInHierarchy) {
        this.positionInHierarchy = positionInHierarchy;
    }

    public int getLevelInParent() {
        return levelInParent;
    }

    /** Retained for compatibility with the original mutable wrapper API. */
    public void setLevelInParent(int levelInParent) {
        this.levelInParent = levelInParent;
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }

    private static String getLastSegment(String className) {
        if (className == null || className.isEmpty()) return "";
        int separator = className.lastIndexOf('.');
        return separator >= 0 ? className.substring(separator + 1) : className;
    }
}
