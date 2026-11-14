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
import com.drhowdydoo.layoutinspector.model.ViewNodeWrapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;

public class Utils {

    private static final float OVERLAY_PARENT_COVERAGE = 0.70f;
    private static final float OVERLAY_OCCLUSION_COVERAGE = 0.95f;
    private static final float MIN_VISIBLE_ALPHA = 0.05f;
    private static final String TAG = "AssistHierarchy";

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

    /**
     * Builds a geometry-correct snapshot for the focused activity's windows.
     *
     * overlayScreenX/Y are the assistant overlay's screen coordinates. WindowNode coordinates
     * are screen-relative, so subtracting this origin maps every result into overlay-canvas space.
     */
    public static HierarchyResult displayViewHierarchy(AssistStructure assistStructure,
                                                        int overlayScreenX,
                                                        int overlayScreenY,
                                                        int expectedDisplayId,
                                                        int requestedViewType) {
        if (assistStructure == null) return HierarchyResult.empty();

        List<TreeNode> roots = new ArrayList<>();
        List<TreeNode> flatNodes = new ArrayList<>();
        int windowNodeCount = assistStructure.getWindowNodeCount();

        for (int i = 0; i < windowNodeCount; i++) {
            throwIfCancelled();
            AssistStructure.WindowNode windowNode = assistStructure.getWindowNodeAt(i);
            if (windowNode == null) continue;
            if (expectedDisplayId >= 0 && windowNode.getDisplayId() != expectedDisplayId) {
                continue;
            }

            AssistStructure.ViewNode rootViewNode = windowNode.getRootViewNode();
            if (rootViewNode == null) continue;

            Log.d(TAG, "Reading window title=" + windowNode.getTitle()
                    + ", display=" + windowNode.getDisplayId()
                    + ", bounds=[" + windowNode.getLeft() + "," + windowNode.getTop()
                    + " " + windowNode.getWidth() + "x" + windowNode.getHeight()
                    + "], root=" + rootViewNode.getClassName());

            int windowWidth = windowNode.getWidth() > 0
                    ? windowNode.getWidth() : rootViewNode.getWidth();
            int windowHeight = windowNode.getHeight() > 0
                    ? windowNode.getHeight() : rootViewNode.getHeight();
            if (windowWidth <= 0 || windowHeight <= 0) continue;

            int windowLeft = windowNode.getLeft() - overlayScreenX;
            int windowTop = windowNode.getTop() - overlayScreenY;
            Rect windowClip = new Rect(windowLeft, windowTop,
                    windowLeft + windowWidth, windowTop + windowHeight);

            Matrix windowToOverlay = new Matrix();
            windowToOverlay.setTranslate(windowLeft, windowTop);

            int windowNodeStart = flatNodes.size();
            TreeNode root = buildViewHierarchy(rootViewNode, 0, null,
                    windowToOverlay, 0, 0, windowClip, true, 1f, null, flatNodes);
            if (root == null) continue;

            // Resolve foreground layers before deciding whether this launcher window actually
            // represents a screen that can be inspected (home, drawer, or overview).
            applySiblingOcclusion(root);

            // Some launchers retain a full hierarchy for the home screen while a separate
            // surface (for example Google Discover) is displayed over it. Incidental nodes such
            // as accessibility_action_view can still have full-screen bounds in AssistStructure.
            // For a recognized launcher hierarchy, require at least one real primary surface to
            // be visible and not occluded. Otherwise discard this entire window rather than
            // drawing stale launcher elements over the unreported surface.
            if (containsLauncherSurfaceMarker(root)
                    && !hasActiveLauncherSurface(root)) {
                Log.d(TAG, "Suppressing launcher window: no visible primary surface. "
                        + "The focused AssistStructure does not expose inspectable content "
                        + "for the currently displayed launcher surface.");
                logUncoveredLauncherCandidates(root);
                flatNodes.subList(windowNodeStart, flatNodes.size()).clear();
                continue;
            }

            roots.add(root);
        }

        // A root-only or entirely clipped/hidden hierarchy is not useful inspection data. Return
        // an empty snapshot instead of drawing a misleading full-screen root rectangle.
        boolean hasForegroundContent = false;
        for (TreeNode treeNode : flatNodes) {
            ViewNodeWrapper wrapper = (ViewNodeWrapper) treeNode.getValue();
            if (wrapper.getDepth() > 0
                    && !wrapper.isOccluded()
                    && !wrapper.getVisibleBounds().isEmpty()
                    && (requestedViewType < 0 || wrapper.showViewNode(requestedViewType))) {
                hasForegroundContent = true;
                break;
            }
        }
        int visibleNodeCount = 0;
        int occludedNodeCount = 0;
        for (TreeNode treeNode : flatNodes) {
            ViewNodeWrapper wrapper = (ViewNodeWrapper) treeNode.getValue();
            if (wrapper.isOccluded()) {
                occludedNodeCount++;
            } else if (wrapper.isVisible() && !wrapper.getVisibleBounds().isEmpty()) {
                visibleNodeCount++;
            }
        }
        Log.d(TAG, "Assist hierarchy result: windows=" + windowNodeCount
                + ", totalNodes=" + flatNodes.size()
                + ", visibleNodes=" + visibleNodeCount
                + ", occludedNodes=" + occludedNodeCount
                + ", usableForegroundContent=" + hasForegroundContent);

        return hasForegroundContent
                ? new HierarchyResult(roots, flatNodes)
                : HierarchyResult.empty();
    }

    private static TreeNode buildViewHierarchy(AssistStructure.ViewNode viewNode,
                                               int depth,
                                               TreeNode parent,
                                               Matrix parentToOverlay,
                                               int parentScrollX,
                                               int parentScrollY,
                                               Rect inheritedClip,
                                               boolean parentVisible,
                                               float parentAlpha,
                                               String parentVisibilityBlocker,
                                               List<TreeNode> flatNodes) {
        throwIfCancelled();

        int width = viewNode.getWidth();
        int height = viewNode.getHeight();
        Matrix localToParent = new Matrix();
        localToParent.setTranslate(
                viewNode.getLeft() - parentScrollX,
                viewNode.getTop() - parentScrollY);

        Matrix nodeTransformation = viewNode.getTransformation();
        if (nodeTransformation != null) {
            // Matrix multiplication order: position * view transformation.
            Matrix positioned = new Matrix();
            positioned.setConcat(localToParent, new Matrix(nodeTransformation));
            localToParent = positioned;
        }

        Matrix localToOverlay = new Matrix();
        localToOverlay.setConcat(parentToOverlay, localToParent);

        Rect transformedBounds = mapLocalBounds(localToOverlay, width, height);
        Rect visibleBounds = new Rect(transformedBounds);
        if (inheritedClip == null || inheritedClip.isEmpty()
                || !visibleBounds.intersect(inheritedClip)) {
            visibleBounds.setEmpty();
        }

        float alpha = parentAlpha * clampAlpha(viewNode.getAlpha());
        boolean isVisible = parentVisible
                && viewNode.getVisibility() == View.VISIBLE
                && width > 0
                && height > 0
                && alpha > MIN_VISIBLE_ALPHA
                && !visibleBounds.isEmpty();

        Rect childClip = new Rect(inheritedClip == null ? new Rect() : inheritedClip);
        if (width <= 0 || height <= 0 || !childClip.intersect(transformedBounds)) {
            childClip.setEmpty();
        }

        Rect parentBounds = new Rect();
        if (parent != null && parent.getValue() instanceof ViewNodeWrapper) {
            parentBounds.set(((ViewNodeWrapper) parent.getValue()).getVisibleBounds());
        }

        int position = flatNodes.size();
        ViewNodeWrapper wrapper = new ViewNodeWrapper(
                viewNode,
                isVisible,
                transformedBounds,
                visibleBounds,
                parentBounds,
                alpha,
                depth,
                depth,
                position);
        wrapper.setVisibilityBlockedBy(parentVisibilityBlocker);

        // Keep focused diagnostics for launcher containers. These logs make it possible to
        // distinguish a genuinely visible drawer from launcher nodes retained off-screen.
        String idEntry = viewNode.getIdEntry();
        if ("workspace".equals(idEntry)
                || "hidden_apps_view".equals(idEntry)
                || "apps_view_container".equals(idEntry)
                || "apps_list_view".equals(idEntry)
                || "overview_panel".equals(idEntry)) {
            Log.d(TAG, "Node id=" + idEntry
                    + ", visibility=" + viewNode.getVisibility()
                    + ", alpha=" + viewNode.getAlpha()
                    + ", elevation=" + viewNode.getElevation()
                    + ", rawLeftTop=" + viewNode.getLeft() + "," + viewNode.getTop()
                    + ", parentScroll=" + parentScrollX + "," + parentScrollY
                    + ", ownScroll=" + viewNode.getScrollX() + "," + viewNode.getScrollY()
                    + ", transformed=" + transformedBounds
                    + ", clipped=" + visibleBounds
                    + ", effectiveVisible=" + isVisible);
        }

        TreeNode treeNode = new TreeNode(wrapper, R.layout.hierarchy_tree_node);
        if (parent != null) parent.addChild(treeNode);
        flatNodes.add(treeNode);

        int childCount = viewNode.getChildCount();
        int childScrollX = viewNode.getScrollX();
        int childScrollY = viewNode.getScrollY();
        for (int i = 0; i < childCount; i++) {
            throwIfCancelled();
            AssistStructure.ViewNode child = viewNode.getChildAt(i);
            if (child != null) {
                String blockerForChildren = parentVisibilityBlocker;
                if (blockerForChildren == null && !isVisible) {
                    blockerForChildren = describeVisibilityBlocker(viewNode);
                }

                buildViewHierarchy(child, depth + 1, treeNode,
                        localToOverlay, childScrollX, childScrollY,
                        childClip, isVisible, alpha,
                        blockerForChildren, flatNodes);
            }
        }
        return treeNode;
    }


    /**
     * Describes the first node in an ancestor chain that makes descendants effectively
     * invisible. The returned value is diagnostic only; it does not affect visibility,
     * bounds, occlusion, or selection.
     */
    private static String describeVisibilityBlocker(AssistStructure.ViewNode viewNode) {
        if (viewNode == null) return "Unknown ancestor";

        String idEntry = viewNode.getIdEntry();
        String className = viewNode.getClassName();

        if (className != null && !className.isEmpty()) {
            int separator = className.lastIndexOf('.');
            if (separator >= 0) {
                className = className.substring(separator + 1);
            }
        } else {
            className = "Unknown View";
        }

        if (idEntry != null && !idEntry.isEmpty()) {
            return "@id/" + idEntry + " (" + className + ")";
        }
        return className + " (no resource ID)";
    }

    private static Rect mapLocalBounds(Matrix localToOverlay, int width, int height) {
        if (width <= 0 || height <= 0) return new Rect();
        RectF local = new RectF(0f, 0f, width, height);
        localToOverlay.mapRect(local);
        if (!isFinite(local.left) || !isFinite(local.top)
                || !isFinite(local.right) || !isFinite(local.bottom)) {
            return new Rect();
        }
        Rect result = new Rect();
        local.roundOut(result);
        return result;
    }

    private static float clampAlpha(float alpha) {
        if (Float.isNaN(alpha)) return 0f;
        return Math.max(0f, Math.min(1f, alpha));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    /**
     * Uses later, full-size sibling containers with actual exposed content as foreground layers.
     * This is intentionally conservative: a full-screen empty placeholder (such as the drawer's
     * empty Compose search-results view) must not hide an earlier populated sibling.
     */
    private static void applySiblingOcclusion(TreeNode parent) {
        LinkedList<TreeNode> linkedChildren = parent.getChildren();
        if (linkedChildren == null || linkedChildren.isEmpty()) return;
        List<TreeNode> children = new ArrayList<>(linkedChildren);

        for (TreeNode child : children) {
            throwIfCancelled();
            applySiblingOcclusion(child);
        }

        Object parentValue = parent.getValue();
        if (!(parentValue instanceof ViewNodeWrapper)) return;
        Rect parentBounds = ((ViewNodeWrapper) parentValue).getVisibleBounds();
        long parentArea = area(parentBounds);
        if (parentArea <= 0L) return;

        for (int foregroundIndex = 0; foregroundIndex < children.size(); foregroundIndex++) {
            TreeNode foreground = children.get(foregroundIndex);
            ViewNodeWrapper foregroundWrapper = getWrapper(foreground);
            if (foregroundWrapper == null || !isOverlayCandidate(foreground, foregroundWrapper,
                    parentArea)) {
                continue;
            }

            Rect foregroundBounds = foregroundWrapper.getVisibleBounds();
            for (int backgroundIndex = 0; backgroundIndex < children.size(); backgroundIndex++) {
                if (backgroundIndex == foregroundIndex) continue;
                TreeNode background = children.get(backgroundIndex);
                ViewNodeWrapper backgroundWrapper = getWrapper(background);
                if (backgroundWrapper == null || backgroundWrapper.isOccluded()
                        || !backgroundWrapper.isVisible()
                        || !isInFront(foregroundWrapper, foregroundIndex,
                                backgroundWrapper, backgroundIndex)) {
                    continue;
                }
                if (covers(foregroundBounds, backgroundWrapper.getVisibleBounds(),
                        OVERLAY_OCCLUSION_COVERAGE)) {
                    Log.d(TAG, "Occluding sibling branch "
                            + backgroundWrapper.getViewNode().getClassName()
                            + "/" + backgroundWrapper.getViewNode().getIdEntry()
                            + " behind " + foregroundWrapper.getViewNode().getClassName()
                            + "/" + foregroundWrapper.getViewNode().getIdEntry());
                    markSubtreeOccluded(background);
                }
            }
        }
    }

    // Stable Launcher3/Nothing launcher surface IDs observed in AssistStructure. These are
    // deliberately used only as a fail-closed guard when the tree identifies itself as a
    // launcher hierarchy; ordinary applications are not required to expose these IDs.
    private static final String[] LAUNCHER_PRIMARY_SURFACE_IDS = {
            "workspace",
            "hidden_apps_view",
            "overview_panel"
    };

    private static boolean containsLauncherSurfaceMarker(TreeNode node) {
        ViewNodeWrapper wrapper = getWrapper(node);
        if (wrapper != null && isLauncherPrimarySurfaceId(
                wrapper.getViewNode().getIdEntry())) {
            return true;
        }
        LinkedList<TreeNode> children = node == null ? null : node.getChildren();
        if (children == null) return false;
        for (TreeNode child : children) {
            if (containsLauncherSurfaceMarker(child)) return true;
        }
        return false;
    }

    private static boolean hasActiveLauncherSurface(TreeNode node) {
        ViewNodeWrapper wrapper = getWrapper(node);
        if (wrapper != null
                && isLauncherPrimarySurfaceId(wrapper.getViewNode().getIdEntry())
                && wrapper.isVisible()
                && !wrapper.isOccluded()
                && !wrapper.getVisibleBounds().isEmpty()) {
            return true;
        }
        LinkedList<TreeNode> children = node == null ? null : node.getChildren();
        if (children == null) return false;
        for (TreeNode child : children) {
            if (hasActiveLauncherSurface(child)) return true;
        }
        return false;
    }

    private static boolean isLauncherPrimarySurfaceId(String idEntry) {
        if (idEntry == null) return false;
        for (String id : LAUNCHER_PRIMARY_SURFACE_IDS) {
            if (id.equals(idEntry)) return true;
        }
        return false;
    }

    /** Logs a few visible but non-primary nodes to make suppressed snapshots diagnosable. */
    private static void logUncoveredLauncherCandidates(TreeNode node) {
        if (node == null) return;
        int[] logged = {0};
        logUncoveredLauncherCandidates(node, logged);
    }

    private static void logUncoveredLauncherCandidates(TreeNode node, int[] logged) {
        if (node == null || logged[0] >= 8) return;
        ViewNodeWrapper wrapper = getWrapper(node);
        if (wrapper != null && wrapper.getDepth() > 0
                && wrapper.isVisible() && !wrapper.isOccluded()
                && !wrapper.getVisibleBounds().isEmpty()) {
            AssistStructure.ViewNode viewNode = wrapper.getViewNode();
            Log.d(TAG, "Ignoring incidental launcher node class=" + viewNode.getClassName()
                    + ", id=" + viewNode.getIdEntry()
                    + ", desc=" + viewNode.getContentDescription()
                    + ", bounds=" + wrapper.getVisibleBounds());
            logged[0]++;
        }
        LinkedList<TreeNode> children = node.getChildren();
        if (children == null) return;
        for (TreeNode child : children) {
            if (logged[0] >= 8) break;
            logUncoveredLauncherCandidates(child, logged);
        }
    }

    private static boolean isOverlayCandidate(TreeNode node,
                                              ViewNodeWrapper wrapper,
                                              long parentArea) {
        if (!wrapper.isVisible() || wrapper.isOccluded()
                || area(wrapper.getVisibleBounds()) < parentArea * OVERLAY_PARENT_COVERAGE) {
            return false;
        }

        // AssistStructure does not expose drawable alpha, but it does expose View alpha.
        // Never let a genuinely translucent View hide the underlying branch in our model.
        if (wrapper.getEffectiveAlpha() < 0.98f || isKnownScrimId(
                wrapper.getViewNode().getIdEntry())) {
            return false;
        }
        return hasVisibleContentLeaf(node);
    }

    private static boolean isKnownScrimId(String idEntry) {
        if (idEntry == null || idEntry.isEmpty()) return false;
        String normalized = idEntry.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("touch_outside")
                || normalized.contains("scrim")
                || normalized.equals("statusbarbackground")
                || normalized.equals("status_bar_background");
    }

    private static boolean hasVisibleContentLeaf(TreeNode node) {
        LinkedList<TreeNode> children = node.getChildren();
        if (children == null || children.isEmpty()) return false;
        for (TreeNode child : children) {
            ViewNodeWrapper wrapper = getWrapper(child);
            if (wrapper == null || !wrapper.isVisible() || wrapper.getVisibleBounds().isEmpty()) {
                continue;
            }
            AssistStructure.ViewNode viewNode = wrapper.getViewNode();
            CharSequence text = viewNode.getText();
            CharSequence description = viewNode.getContentDescription();
            String idEntry = viewNode.getIdEntry();
            boolean hasSemanticContent = (text != null && text.length() > 0)
                    || (description != null && description.length() > 0)
                    || (idEntry != null && !idEntry.isEmpty())
                    || viewNode.isClickable()
                    || viewNode.isFocusable();
            if (hasSemanticContent) return true;
            if (hasVisibleContentLeaf(child)) return true;
        }
        return false;
    }


    /**
     * AssistStructure exposes elevation but not ViewGroup's custom child drawing order or
     * translationZ separately. Prefer higher elevation; for equal elevation use hierarchy order
     * as the best available approximation of the launcher's drawing order.
     */
    private static boolean isInFront(ViewNodeWrapper foreground,
                                     int foregroundIndex,
                                     ViewNodeWrapper background,
                                     int backgroundIndex) {
        float foregroundElevation = foreground.getViewNode().getElevation();
        float backgroundElevation = background.getViewNode().getElevation();
        if (foregroundElevation > backgroundElevation + 0.01f) return true;
        if (backgroundElevation > foregroundElevation + 0.01f) return false;
        return foregroundIndex > backgroundIndex;
    }

    private static boolean covers(Rect foreground, Rect background, float requiredCoverage) {
        long backgroundArea = area(background);
        if (backgroundArea <= 0L) return false;
        Rect intersection = new Rect(foreground);
        if (!intersection.intersect(background)) return false;
        return area(intersection) >= backgroundArea * requiredCoverage;
    }

    private static long area(Rect rect) {
        if (rect == null || rect.isEmpty()) return 0L;
        return (long) rect.width() * (long) rect.height();
    }

    private static void markSubtreeOccluded(TreeNode node) {
        ViewNodeWrapper wrapper = getWrapper(node);
        if (wrapper != null) wrapper.setOccluded(true);
        LinkedList<TreeNode> children = node.getChildren();
        if (children == null) return;
        for (TreeNode child : children) markSubtreeOccluded(child);
    }

    private static ViewNodeWrapper getWrapper(TreeNode node) {
        if (node == null || !(node.getValue() instanceof ViewNodeWrapper)) return null;
        return (ViewNodeWrapper) node.getValue();
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
