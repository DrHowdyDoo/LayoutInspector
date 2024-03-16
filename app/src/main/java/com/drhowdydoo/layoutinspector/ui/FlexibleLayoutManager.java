package com.drhowdydoo.layoutinspector.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FlexibleLayoutManager extends LinearLayoutManager {
    private int offset;
    private int maxOffset;

    public FlexibleLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        int n = getChildCount();
        offset = 0;
        maxOffset = 0;
        int ownWidth = getWidth();
        for(int i=0; i<n; ++i) {
            View view = getChildAt(i);
            int x = view.getRight();
            if(x>ownWidth) maxOffset = Math.max(maxOffset,x-ownWidth) + 30;
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if(dx<0) {
            if(-dx>offset) dx = -offset;
        }
        else
        if(dx>0) {
            if(dx+offset>maxOffset) dx = maxOffset-offset;
        }
        offsetChildrenHorizontal(-dx);
        offset += dx;
        return dx;
    }

}
