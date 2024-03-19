package com.drhowdydoo.layoutinspector.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drhowdydoo.layoutinspector.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class DrawableFrameLayout extends FrameLayout {

    public final Rect visibleDisplayFrame = new Rect();
    private Paint singleBoundPaint, multiBoundPaint;
    private Rect rect = new Rect();
    private List<Rect> layoutBounds = new ArrayList<>();

    public DrawableFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        singleBoundPaint = new Paint();
        singleBoundPaint.setAntiAlias(true);
        singleBoundPaint.setColor(Color.GREEN);
        singleBoundPaint.setStyle(Paint.Style.STROKE);
        singleBoundPaint.setStrokeWidth(5.5f);

        multiBoundPaint = new Paint();
        multiBoundPaint.setAntiAlias(true);
        multiBoundPaint.setColor(Color.GREEN);
        multiBoundPaint.setStyle(Paint.Style.STROKE);
        multiBoundPaint.setStrokeWidth(2.5f);
    }

    public DrawableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void updatePaintAttributes() {
        multiBoundPaint.setColor(PreferenceManager.strokeColor);
        multiBoundPaint.setStrokeWidth(PreferenceManager.strokeWidth);
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
        canvas.drawRect(rect, singleBoundPaint);
        if (PreferenceManager.showLayoutBounds) drawLayoutBounds(canvas);
        else clearCanvas(canvas);
    }

    private void drawLayoutBounds(Canvas canvas) {
        for (Rect rect : layoutBounds) {
            canvas.drawRect(rect, multiBoundPaint);
        }
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    public void drawRect(Rect rect) {
        this.rect = rect;
        invalidate();
    }


    public void updateLayoutBounds(List<Rect> layoutBounds) {
        this.layoutBounds = layoutBounds;
    }

}
