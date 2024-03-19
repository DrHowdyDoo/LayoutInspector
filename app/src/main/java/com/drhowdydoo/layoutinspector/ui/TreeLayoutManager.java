package com.drhowdydoo.layoutinspector.ui;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TreeLayoutManager extends LinearLayoutManager {
    private int offset;
    private int maxOffset;

    public TreeLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        setMaxOffSet();
    }

    private void setMaxOffSet(){
        int n = getChildCount();
        maxOffset = 0;
        for(int i=0; i<n; ++i) {
            View view = getChildAt(i);
            if (view != null) updateMaxOffset(view);
        }
        if(offset>maxOffset) offset = maxOffset;
        offsetChildren();
    }

    private void offsetChildren() {
        int n = getChildCount();
        for(int i=0; i<n; ++i) {
            View view = getChildAt(i);
            view.setTranslationX(-offset);
        }
    }

    private void updateMaxOffset(View view) {
        int x = view.getRight();
        int ownWidth = getWidth();
        if(x>ownWidth) maxOffset = Math.max(maxOffset,x-ownWidth);
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        child.setTranslationX(-offset);
        child.post(()->updateMaxOffset(child));
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
