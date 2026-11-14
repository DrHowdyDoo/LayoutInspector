package com.drhowdydoo.layoutinspector.model;

import android.app.assist.AssistStructure;
import android.graphics.Rect;
import android.view.View;

/**
 * Immutable geometry snapshot for one AssistStructure.ViewNode.
 *
 * bounds contains the transformed, unclipped screen rectangle. visibleBounds contains that
 * rectangle clipped to the window and all ancestor bounds. Drawing and hit testing must use
 * visibleBounds so content outside a scroll viewport or behind a clipped parent is not selected.
 */
public class ViewNodeWrapper {

    private AssistStructure.ViewNode viewNode;
    private boolean visibility;
    private final Rect bounds;
    private final Rect visibleBounds;
    private final Rect parentBounds;
    private boolean occluded;
    private float effectiveAlpha = 1f;
    private int depth;
    private int levelInParent;
    private int positionInHierarchy;
    private String simpleClassName;
    private ArrowSet arrowSet;

    public ViewNodeWrapper(AssistStructure.ViewNode viewNode,
                           boolean visibility,
                           Rect bounds,
                           Rect visibleBounds,
                           Rect parentBounds,
                           int depth,
                           int levelInParent,
                           int positionInHierarchy) {
        this(viewNode, visibility, bounds, visibleBounds, parentBounds,
                viewNode != null ? viewNode.getAlpha() : 1f,
                depth, levelInParent, positionInHierarchy);
    }

    public ViewNodeWrapper(AssistStructure.ViewNode viewNode,
                           boolean visibility,
                           Rect bounds,
                           Rect visibleBounds,
                           Rect parentBounds,
                           float effectiveAlpha,
                           int depth,
                           int levelInParent,
                           int positionInHierarchy) {
        this.viewNode = viewNode;
        this.visibility = visibility;
        this.bounds = bounds == null ? new Rect() : new Rect(bounds);
        this.visibleBounds = visibleBounds == null ? new Rect() : new Rect(visibleBounds);
        this.parentBounds = parentBounds == null ? new Rect() : new Rect(parentBounds);
        this.effectiveAlpha = Math.max(0f, Math.min(1f, effectiveAlpha));
        this.depth = depth;
        this.levelInParent = levelInParent;
        this.positionInHierarchy = positionInHierarchy;
        this.simpleClassName = getLastSegment(viewNode != null ? viewNode.getClassName() : null);
    }

    /** Compatibility constructor retained for callers outside this source set. */
    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility) {
        this(viewNode, visibility, new Rect(), new Rect(), new Rect(), 0, 0, 0);
    }

    /** Compatibility constructor retained for callers outside this source set. */
    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility, ArrowSet arrowSet) {
        this(viewNode, visibility);
        this.arrowSet = arrowSet;
    }

    /** Compatibility constructor for the previous hierarchy builder. */
    public ViewNodeWrapper(AssistStructure.ViewNode viewNode,
                           boolean visibility,
                           Rect bounds,
                           int depth,
                           int levelInParent,
                           int positionInHierarchy,
                           AssistStructure.ViewNode parentViewNode,
                           int parentLeft,
                           int parentTop) {
        this(viewNode, visibility, bounds, bounds, new Rect(), depth,
                levelInParent, positionInHierarchy);
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

    /** Returns the ancestor-clipped rectangle used for drawing and touch hit testing. */
    public Rect getBounds() {
        return visibleBounds;
    }

    public Rect getVisibleBounds() {
        return visibleBounds;
    }

    /** Returns the transformed rectangle before clipping by ancestors/window edges. */
    public Rect getUnclippedBounds() {
        return bounds;
    }

    public Rect getParentBounds() {
        return parentBounds;
    }

    public boolean isOccluded() {
        return occluded;
    }

    /** Alpha after combining this node with all of its ancestors. */
    public float getEffectiveAlpha() {
        return effectiveAlpha;
    }

    public void setOccluded(boolean occluded) {
        this.occluded = occluded;
    }

    /**
     * Distances are calculated from the transformed child bounds to its transformed parent
     * bounds. This avoids mixing raw ViewNode coordinates with scrolled/transformed coordinates.
     */
    public ArrowSet getArrowSet() {
        if (arrowSet == null) {
            if (parentBounds.isEmpty() || visibleBounds.isEmpty()) {
                arrowSet = new ArrowSet();
            } else {
                Rect child = visibleBounds;
                int centerX = (child.left + child.right) / 2;
                int centerY = (child.top + child.bottom) / 2;
                arrowSet = new ArrowSet(
                        new Arrow(child.left, centerY, parentBounds.left, centerY),
                        new Arrow(child.right, centerY, parentBounds.right, centerY),
                        new Arrow(centerX, child.top, centerX, parentBounds.top),
                        new Arrow(centerX, child.bottom, centerX, parentBounds.bottom));
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
