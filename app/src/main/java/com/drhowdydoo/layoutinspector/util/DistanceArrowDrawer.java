package com.drhowdydoo.layoutinspector.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.drhowdydoo.layoutinspector.model.ArrowSet;

public class DistanceArrowDrawer {
    private static Paint textPaint;
    private static Paint textBoxPaint;
    private static ArrowSet arrowSet;
    private static RectF rectF;
    private static final int ARROW_LEFT = 0;
    private static final int ARROW_TOP = 1;
    private static final int ARROW_RIGHT = 2;
    private static final int ARROW_BOTTOM = 3;


    public static void init(Context context, int paintColor, float strokeWidth){

        ArrowDrawer.init(context, paintColor, strokeWidth);
        arrowSet = new ArrowSet();

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Utils.spToPx(context, 14));
        textPaint.setTextAlign(Paint.Align.CENTER);

        textBoxPaint = new Paint();
        textBoxPaint.setStyle(Paint.Style.FILL);
        textBoxPaint.setAntiAlias(true);
        textBoxPaint.setColor(paintColor);
        textBoxPaint.setAlpha(75);
    }

    public static void notifyPaintChange(int color, float width){
        ArrowDrawer.notifyPaintChange(color, width);
        textBoxPaint.setColor(color);
        textBoxPaint.setAlpha(70);
    }

    public static void onDraw(Canvas canvas, Context context) {
        if (arrowSet == null) {
            return;
        }

        ArrowDrawer.onDraw(canvas);
        float topTextWidth = textPaint.measureText(arrowSet.getTopArrow().length() + " dp");
        float bottomTextWidth = textPaint.measureText(arrowSet.getBottomArrow().length() + " dp");

        drawDistanceLabel(context, canvas,
                    arrowSet.getLeftArrow().getCenterX(),
                    arrowSet.getLeftArrow().getCenterY() - Utils.dpToPx(context, 15),
                    arrowSet.getLeftArrow().length(),
                    ARROW_LEFT);

        drawDistanceLabel(context, canvas,
                    arrowSet.getTopArrow().getCenterX() + (topTextWidth / 2) + Utils.dpToPx(context, 6),
                    arrowSet.getTopArrow().getCenterY() - Utils.dpToPx(context, 6),
                    arrowSet.getTopArrow().length(),
                    ARROW_TOP);

        drawDistanceLabel(context, canvas,
                    arrowSet.getRightArrow().getCenterX(),
                    arrowSet.getRightArrow().getCenterY() - Utils.dpToPx(context, 15),
                    arrowSet.getRightArrow().length(),
                    ARROW_RIGHT);

        drawDistanceLabel(context, canvas,
                    arrowSet.getBottomArrow().getCenterX() + (bottomTextWidth / 2) + Utils.dpToPx(context, 6),
                    arrowSet.getBottomArrow().getCenterY() + Utils.dpToPx(context, 6),
                    arrowSet.getBottomArrow().length(),
                    ARROW_BOTTOM);
    }

    private static void drawDistanceLabel(Context context,
                                          Canvas canvas,
                                          float x,
                                          float y,
                                          float distance,
                                          int arrowDirection){
        if (distance == 0 ) return;
        float width = textPaint.measureText(distance + " dp") + Utils.dpToPx(context, 3);
        float height = Utils.dpToPx(context, 22);
        float left = x - (width / 2f);
        float top = y - (height / 2f);
        float right = x + (width / 2f);
        float bottom = y + (height / 2f);
        rectF = new RectF(left, top, right, bottom);
        adjustLabel(canvas, distance, arrowDirection);
        applyWindowsInset(canvas);
        drawDistanceLabel(context,canvas, rectF, distance);
    }

    private static void drawDistanceLabel(Context context, Canvas canvas, RectF rectF, float distance){
        float radius = rectF.height() / 2f;
        canvas.drawRoundRect(rectF, radius, radius, textBoxPaint);
        float x = (rectF.left + rectF.right) / 2;
        float y = (rectF.top + rectF.bottom) / 2 + (textPaint.getTextSize() / 3) ;
        canvas.drawText(String.format("%s dp", Utils.pxToDp(context, distance)), x, y, textPaint);
    }

    private static void applyWindowsInset(Canvas canvas){
        if (rectF.left < 0) {
            rectF.right += rectF.right - Math.abs(rectF.left);
            rectF.left = 0;
        }
        if (rectF.right > canvas.getWidth()) {
            rectF.left -= rectF.right - canvas.getWidth();
            rectF.right = canvas.getWidth();
        }
        if (rectF.top < 0) {
            rectF.bottom += rectF.bottom - Math.abs(rectF.top);
            rectF.top = 0;
        }
        if (rectF.bottom > canvas.getHeight()) {
            rectF.top -= rectF.bottom - canvas.getHeight();
            rectF.bottom = canvas.getHeight();
        }
    }

    private static void adjustLabel(Canvas canvas, float distance, int arrowDirection) {
        switch (arrowDirection) {
            case ARROW_RIGHT:
                if (rectF.width() > distance) {
                    float offset = rectF.width() - distance;
                    if (offset + rectF.right < canvas.getWidth()) {
                        rectF.offset(offset, 0);
                    }
                }
                break;
            case ARROW_LEFT:
                if (rectF.width() > distance) {
                    float offset = rectF.width() - distance;
                    if (rectF.left - offset > 0) {
                        rectF.offset(-offset, 0);
                    }
                }
                break;
            case ARROW_TOP:
                if (rectF.height() > distance)  {
                    float offset = rectF.height() - distance;
                    if (rectF.top - offset > 0) {
                        rectF.offset(0, -offset);
                    }
                }
                break;
            case ARROW_BOTTOM:
                if (rectF.height() > distance) {
                    float offset = rectF.height() - distance;
                    if (rectF.bottom + offset < canvas.getHeight()) {
                        rectF.offset(0, offset);
                    }
                }
                break;
        }
    }

    public static void clearCanvas(){
        ArrowDrawer.clearCanvas();
    }

    public static void setArrowSet(ArrowSet newArrowSet) {
        arrowSet = newArrowSet;
        ArrowDrawer.setArrowSet(newArrowSet);
    }

}
