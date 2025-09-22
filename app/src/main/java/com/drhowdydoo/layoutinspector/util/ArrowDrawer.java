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
        arrowPaint.setAntiAlias(true);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(strokeWidth);

        arrowHeadPaint = new Paint();
        arrowHeadPaint.setAntiAlias(true);
        arrowHeadPaint.setStyle(Paint.Style.FILL);
        arrowHeadPaint.setColor(paintColor);
    }

    public static void notifyPaintChange(int color, float width){
        arrowPaint.setColor(color);
        arrowPaint.setStrokeWidth(width);
        arrowHeadPaint.setColor(color);
    }

    public static void onDraw(Canvas canvas) {
        if (arrowSet == null) return;
        drawArrow(canvas,
                arrowSet.getLeftArrow().getStartX(),
                arrowSet.getLeftArrow().getStartY(),
                arrowSet.getLeftArrow().getEndX(),
                arrowSet.getLeftArrow().getEndY(),
                8, 0,
                0, 0);

        drawArrow(canvas,
                arrowSet.getTopArrow().getStartX(),
                arrowSet.getTopArrow().getStartY(),
                arrowSet.getTopArrow().getEndX(),
                arrowSet.getTopArrow().getEndY(),
                0, 8,
                0, 0);

        drawArrow(canvas,
                arrowSet.getRightArrow().getStartX(),
                arrowSet.getRightArrow().getStartY(),
                arrowSet.getRightArrow().getEndX(),
                arrowSet.getRightArrow().getEndY(),
                0, 0,
                8, 0);

        drawArrow(canvas,
                arrowSet.getBottomArrow().getStartX(),
                arrowSet.getBottomArrow().getStartY(),
                arrowSet.getBottomArrow().getEndX(),
                arrowSet.getBottomArrow().getEndY(),
                0, 0,
                0, 8);
    }

    private static void drawArrow(Canvas canvas,
                                  float startX, float startY,
                                  float endX, float endY,
                                  float leftOffset, float topOffset,
                                  float rightOffset, float bottomOffset) {

        if (startX == endX && startY == endY) {
            return;
        }
        arrowPath.reset();
        arrowPath.moveTo(startX, startY);
        arrowPath.lineTo(endX, endY);
        canvas.drawPath(arrowPath, arrowPaint);

        double angle = Math.atan2(endY - startY, endX - startX);
        drawArrowHead(canvas, endX - leftOffset + rightOffset, endY - topOffset + bottomOffset, angle);

    }

    private static void drawArrowHead(Canvas canvas,
                                      float endX,
                                      float endY,
                                      double angle){

        // Length of the arrow head
        float arrowHeadLength = 16f;

        // Shift the arrow tip slightly back so it aligns with the line border
        float tipX = endX - (float) (arrowHeadLength * 0.5 * Math.cos(angle));
        float tipY = endY - (float) (arrowHeadLength * 0.5 * Math.sin(angle));

        // Coordinates of the arrow head
        float arrowHeadX1 = tipX - arrowHeadLength * (float) Math.cos(angle - Math.PI / 6);
        float arrowHeadY1 = tipY - arrowHeadLength * (float) Math.sin(angle - Math.PI / 6);

        float arrowHeadX2 = tipX - arrowHeadLength * (float) Math.cos(angle + Math.PI / 6);
        float arrowHeadY2 = tipY - arrowHeadLength * (float) Math.sin(angle + Math.PI / 6);

        arrowPath.moveTo(tipX, tipY);
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
