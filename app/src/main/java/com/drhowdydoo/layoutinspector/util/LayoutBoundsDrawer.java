package com.drhowdydoo.layoutinspector.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class LayoutBoundsDrawer {

    private static Paint singleBoundPaint, multiBoundPaint, fillPaint;
    private static Rect rect = new Rect();
    private static List<Rect> layoutBounds = new ArrayList<>();
    private static PorterDuffColorFilter colorFilter;

    public static void init(Context context, int strokeColor, float strokeWidth) {

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
    }

    public static void notifyPaintChange(int color, float width){
        singleBoundPaint.setColor(color);
        singleBoundPaint.setStrokeWidth(width);
        multiBoundPaint.setColor(color);
        multiBoundPaint.setStrokeWidth(width);
        colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        fillPaint.setColorFilter(colorFilter);
    }

    public static void onDraw(Canvas canvas, boolean showLayoutBounds){
        if (showLayoutBounds) {
            drawLayoutBounds(canvas);
            canvas.drawRect(rect, fillPaint);
        }
        if (rect != null) canvas.drawRect(rect, singleBoundPaint);
    }

    private static void drawLayoutBounds(Canvas canvas) {
        for (Rect bounds : layoutBounds) {
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0
                    && !canvas.quickReject(bounds.left, bounds.top, bounds.right, bounds.bottom, Canvas.EdgeType.BW)) {
                canvas.drawRect(bounds, multiBoundPaint);
            }
        }
    }

    public static void updateLayoutBounds(List<Rect> newLayoutBounds) {
        // Assist snapshots are immutable after publication. Copy the rectangles as well as the
        // list so callers cannot mutate the geometry while the overlay is drawing it.
        List<Rect> copiedBounds = new ArrayList<>();
        if (newLayoutBounds != null) {
            for (Rect bounds : newLayoutBounds) {
                if (bounds != null && !bounds.isEmpty()) {
                    copiedBounds.add(new Rect(bounds));
                }
            }
        }
        layoutBounds = copiedBounds;
    }

    public static void drawRect(Rect newRect){
        if (newRect == null) {
            rect.setEmpty();
        } else {
            // Keep the overlay's current selection separate from the immutable hierarchy bounds.
            rect.set(newRect);
        }
    }

    public static void clearCanvas(){
        layoutBounds.clear();
        rect.set(0,0,0,0);
    }

}
