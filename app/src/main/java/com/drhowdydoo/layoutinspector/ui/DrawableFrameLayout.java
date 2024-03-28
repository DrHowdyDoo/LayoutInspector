package com.drhowdydoo.layoutinspector.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drhowdydoo.layoutinspector.model.ArrowSet;

import java.util.ArrayList;
import java.util.List;

public class DrawableFrameLayout extends FrameLayout {

    public final Rect visibleDisplayFrame = new Rect();
    private Paint singleBoundPaint, multiBoundPaint, fillPaint;
    private Rect rect = new Rect();
    private List<Rect> layoutBounds = new ArrayList<>();
    private SharedPreferences preferences;
    private PorterDuffColorFilter colorFilter;
    private Path arrowPath;
    private Paint arrowPaint;
    private ArrowSet arrowSet;



    public DrawableFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {

        preferences = getContext().getSharedPreferences("com.drhowdydoo.layoutinspector.preferences", MODE_PRIVATE);
        int strokeColor = preferences.getInt("SETTINGS_STROKE_COLOR", Color.GREEN);
        float strokeWidth = preferences.getFloat("SETTINGS_STROKE_WIDTH", 2.5f);

        colorFilter = new PorterDuffColorFilter(strokeColor, PorterDuff.Mode.SRC_IN);

        singleBoundPaint = new Paint();
        singleBoundPaint.setAntiAlias(true);
        singleBoundPaint.setColor(strokeColor);
        singleBoundPaint.setStyle(Paint.Style.STROKE);
        singleBoundPaint.setStrokeWidth(strokeWidth);

        multiBoundPaint = new Paint();
        multiBoundPaint.setAntiAlias(true);
        multiBoundPaint.setColor(strokeColor);
        multiBoundPaint.setStyle(Paint.Style.STROKE);
        multiBoundPaint.setStrokeWidth(strokeWidth);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColorFilter(colorFilter);
        fillPaint.setAlpha(60);

        arrowPath = new Path();

        arrowPaint = new Paint();
        arrowPaint.setColor(strokeColor);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(strokeWidth);

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
        int strokeColor = preferences.getInt("SETTINGS_STROKE_COLOR", Color.GREEN);
        float strokeWidth = preferences.getFloat("SETTINGS_STROKE_WIDTH", 2.5f);
        singleBoundPaint.setColor(strokeColor);
        singleBoundPaint.setStrokeWidth(strokeWidth);
        multiBoundPaint.setColor(strokeColor);
        multiBoundPaint.setStrokeWidth(strokeWidth);
        colorFilter = new PorterDuffColorFilter(strokeColor, PorterDuff.Mode.SRC_IN);
        fillPaint.setColorFilter(colorFilter);
        arrowPaint.setColor(strokeColor);
        arrowPaint.setStrokeWidth(strokeWidth);

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
        boolean showLayoutBounds = preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", false);
        if (showLayoutBounds) {
            drawLayoutBounds(canvas);
            canvas.drawRect(rect, fillPaint);
        }
        if (rect != null) canvas.drawRect(rect, singleBoundPaint);

        if (arrowSet != null) {
            drawArrow(canvas, arrowSet.getLeftArrow().getStartX(), arrowSet.getLeftArrow().getStartY(), arrowSet.getLeftArrow().getEndX(), arrowSet.getLeftArrow().getEndY());
            drawArrow(canvas, arrowSet.getTopArrow().getStartX(), arrowSet.getTopArrow().getStartY(), arrowSet.getTopArrow().getEndX(), arrowSet.getTopArrow().getEndY());
            drawArrow(canvas, arrowSet.getRightArrow().getStartX(), arrowSet.getRightArrow().getStartY(), arrowSet.getRightArrow().getEndX(), arrowSet.getRightArrow().getEndY());
            drawArrow(canvas, arrowSet.getBottomArrow().getStartX(), arrowSet.getBottomArrow().getStartY(), arrowSet.getBottomArrow().getEndX(), arrowSet.getBottomArrow().getEndY());
        }

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

    public void clearCanvas(){
        layoutBounds.clear();
        rect.set(0,0,0,0);
    }

    public void updateLayoutBounds(List<Rect> layoutBounds) {
        this.layoutBounds = layoutBounds;
    }

    private void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY) {
        arrowPath.reset();
        arrowPath.moveTo(startX, startY);
        arrowPath.lineTo(endX, endY);
        canvas.drawPath(arrowPath, arrowPaint);
    }

    public void setArrowSet(ArrowSet arrowSet) {
        this.arrowSet = arrowSet;
    }

}
