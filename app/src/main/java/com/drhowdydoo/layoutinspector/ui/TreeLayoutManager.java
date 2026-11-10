package com.drhowdydoo.layoutinspector.ui;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Linear layout manager with a measured horizontal content offset. */
public class TreeLayoutManager extends LinearLayoutManager {
    private int offset;
    private int maxOffset;

    public TreeLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        recalculateMaxOffset();
    }

    private void recalculateMaxOffset() {
        int childCount = getChildCount();
        int newMaxOffset = 0;
        int ownWidth = getWidth();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != null) {
                int right = child.getRight();
                if (right > ownWidth) newMaxOffset = Math.max(newMaxOffset, right - ownWidth);
            }
        }
        maxOffset = newMaxOffset;
        if (offset > maxOffset) offset = maxOffset;
        applyOffsetToAttachedChildren();
    }

    private void applyOffsetToAttachedChildren() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != null) child.setTranslationX(-offset);
        }
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        child.setTranslationX(-offset);
        // Do not post one Runnable per attached child. The next layout pass measures all of them.
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        if (dx < 0) {
            dx = Math.max(dx, -offset);
        } else if (dx > 0) {
            dx = Math.min(dx, maxOffset - offset);
        }
        if (dx == 0) return 0;

        offsetChildrenHorizontal(-dx);
        offset += dx;
        return dx;
    }
}
