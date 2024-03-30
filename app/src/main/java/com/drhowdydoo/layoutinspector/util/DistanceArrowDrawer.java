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
            drawText(context, canvas,
                    arrowSet.getLeftArrow().getCenterX(),
                    arrowSet.getLeftArrow().getCenterY() - Utils.dpToPx(context, 15),
                    arrowSet.getLeftArrow().length());

            drawText(context, canvas,
                    arrowSet.getTopArrow().getCenterX() + (topTextWidth / 2) + Utils.dpToPx(context, 6),
                    arrowSet.getTopArrow().getCenterY() - Utils.dpToPx(context, 6),
                    arrowSet.getTopArrow().length());

            drawText(context, canvas,
                    arrowSet.getRightArrow().getCenterX(),
                    arrowSet.getRightArrow().getCenterY() - Utils.dpToPx(context, 15),
                    arrowSet.getRightArrow().length());

            drawText(context, canvas,
                    arrowSet.getBottomArrow().getCenterX() + (bottomTextWidth / 2) + Utils.dpToPx(context, 6),
                    arrowSet.getBottomArrow().getCenterY() + Utils.dpToPx(context, 6),
                    arrowSet.getBottomArrow().length());
    }

    private static void drawText(Context context , Canvas canvas, float x, float y, int distance) {
        if (distance == 0) return;

        float textWidth = textPaint.measureText(distance + " dp");

        float width = textWidth + Utils.dpToPx(context, 3);
        int height = Utils.dpToPx(context, 22);

        float radius = height / 2f;
        float left = x - (width / 2f);
        float top = y - (height / 2f);
        float right = x + (width / 2f);
        float bottom = y + (height / 2f);
        float leftOffset = 0;
        float rightOffset = 0;
        float topOffset = 0;
        float bottomOffset = 0;

        if (left < 0) {
            leftOffset = right - Math.abs(left);
            right += leftOffset;
            left = 0;
        }
        if (right > canvas.getWidth()) {
            rightOffset = right - canvas.getWidth();
            left -= rightOffset;
            right = canvas.getWidth();
        }
        if (top < 0) {
            topOffset = bottom - Math.abs(top);
            bottom += topOffset;
            top = 0;
        }
        if (bottom > canvas.getHeight()) {
            bottomOffset = bottom - canvas.getHeight();
            top -= bottomOffset;
            bottom = canvas.getHeight();
        }

        RectF rectF = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rectF, radius, radius, textBoxPaint);

        float startX = x + leftOffset - rightOffset;
        float startY = ((bottom + top)/2) + (textPaint.getTextSize() / 3) + topOffset - bottomOffset;

        canvas.drawText(String.format("%s dp", Utils.pxToDp(context, distance)), startX, startY, textPaint);
    }



    public static void clearCanvas(){
        ArrowDrawer.clearCanvas();
    }

    public static void setArrowSet(ArrowSet newArrowSet) {
        arrowSet = newArrowSet;
        ArrowDrawer.setArrowSet(newArrowSet);
    }

}
