package com.drhowdydoo.layoutinspector.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drhowdydoo.layoutinspector.util.DrawableLayoutHelper;
import com.drhowdydoo.layoutinspector.util.LayoutBoundsDrawer;

public class DrawableFrameLayout extends FrameLayout {

    public final Rect visibleDisplayFrame = new Rect();

    public DrawableFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public DrawableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        DrawableLayoutHelper.init(getContext());
    }

    public void notifyPaintChange() {
        DrawableLayoutHelper.notifyPaintChange();
        invalidate();
    }

    public void notifyPreferenceChange(){
        DrawableLayoutHelper.notifyPreferenceChange();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getWindowVisibleDisplayFrame(visibleDisplayFrame);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        DrawableLayoutHelper.onDraw(canvas, getContext());
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    public void drawRect(Rect rect) {
        LayoutBoundsDrawer.drawRect(rect);
        invalidate();
    }

}
