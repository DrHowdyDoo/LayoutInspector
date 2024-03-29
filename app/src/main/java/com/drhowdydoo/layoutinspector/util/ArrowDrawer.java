package com.drhowdydoo.layoutinspector.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.drhowdydoo.layoutinspector.model.ArrowSet;

public class ArrowDrawer {

    private static Path arrowPath;
    private static Paint arrowPaint;
    private static ArrowSet arrowSet;
    private static Paint arrowHeadPaint;

    public static void init(Context context, int paintColor, float strokeWidth) {
        arrowPath = new Path();
        arrowSet = new ArrowSet();

        arrowPaint = new Paint();
        arrowPaint.setColor(paintColor);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(strokeWidth);

        arrowHeadPaint = new Paint();
        arrowHeadPaint.setStyle(Paint.Style.FILL);
        arrowHeadPaint.setColor(paintColor);
    }

    public static void notifyPaintChange(int color, float width){
        arrowPaint.setColor(color);
        arrowPaint.setStrokeWidth(width);
        arrowHeadPaint.setColor(color);
    }

    public static void onDraw(Canvas canvas) {
        drawArrow(canvas, arrowSet.getLeftArrow().getStartX(), arrowSet.getLeftArrow().getStartY(), arrowSet.getLeftArrow().getEndX(), arrowSet.getLeftArrow().getEndY());
        drawArrow(canvas, arrowSet.getTopArrow().getStartX(), arrowSet.getTopArrow().getStartY(), arrowSet.getTopArrow().getEndX(), arrowSet.getTopArrow().getEndY());
        drawArrow(canvas, arrowSet.getRightArrow().getStartX(), arrowSet.getRightArrow().getStartY(), arrowSet.getRightArrow().getEndX(), arrowSet.getRightArrow().getEndY());
        drawArrow(canvas, arrowSet.getBottomArrow().getStartX(), arrowSet.getBottomArrow().getStartY(), arrowSet.getBottomArrow().getEndX(), arrowSet.getBottomArrow().getEndY());
    }

    private static void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY) {
        if (startX == endX && startY == endY) {
            return;
        }
        arrowPath.reset();
        arrowPath.moveTo(startX, startY);
        arrowPath.lineTo(endX, endY);
        canvas.drawPath(arrowPath, arrowPaint);

        double angle = Math.atan2(endY - startY, endX - startX);
        // Length of the arrow head
        float arrowHeadLength = 16;
        // Coordinates of the arrow head
        float arrowHeadX1 = endX - arrowHeadLength * (float) Math.cos(angle - Math.PI / 6);
        float arrowHeadY1 = endY - arrowHeadLength * (float) Math.sin(angle - Math.PI / 6);
        float arrowHeadX2 = endX - arrowHeadLength * (float) Math.cos(angle + Math.PI / 6);
        float arrowHeadY2 = endY - arrowHeadLength * (float) Math.sin(angle + Math.PI / 6);

        arrowPath.moveTo(endX, endY);
        arrowPath.lineTo(arrowHeadX1, arrowHeadY1);
        arrowPath.lineTo(arrowHeadX2, arrowHeadY2);
        arrowPath.close();

        canvas.drawPath(arrowPath, arrowHeadPaint);
    }

    public static void setArrowSet(ArrowSet newArrowSet) {
        arrowSet = newArrowSet;
    }

    public static void clearCanvas(){
        arrowSet.clear();
    }

}
