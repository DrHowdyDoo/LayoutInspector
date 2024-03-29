package com.drhowdydoo.layoutinspector.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.drhowdydoo.layoutinspector.model.ArrowSet;

public class DistanceArrowDrawer {
    private static Paint textPaint;
    private static Paint textBoxPaint;
    private static Path textPath;
    private static ArrowSet arrowSet;

    public static void init(Context context, int paintColor, float strokeWidth){

        ArrowDrawer.init(context, paintColor, strokeWidth);
        arrowSet = new ArrowSet();

        textPath = new Path();

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Utils.spToPx(context, 14));
        textPaint.setTextAlign(Paint.Align.CENTER);

        textBoxPaint = new Paint();
        textBoxPaint.setStyle(Paint.Style.FILL);
        textBoxPaint.setAntiAlias(true);
        textBoxPaint.setColor(paintColor);
        textBoxPaint.setAlpha(70);
    }

    public static void notifyPaintChange(int color, float width){
        ArrowDrawer.notifyPaintChange(color, width);
        textBoxPaint.setColor(color);
        textBoxPaint.setAlpha(70);
    }

    public static void onDraw(Canvas canvas, Context context) {
            ArrowDrawer.onDraw(canvas);
            drawText(context, canvas, arrowSet.getLeftArrow().getCenterX(), arrowSet.getLeftArrow().getCenterY(), arrowSet.getLeftArrow().length());
            drawText(context, canvas, arrowSet.getTopArrow().getCenterX(), arrowSet.getTopArrow().getCenterY(), arrowSet.getTopArrow().length());
            drawText(context, canvas, arrowSet.getRightArrow().getCenterX(), arrowSet.getRightArrow().getCenterY(), arrowSet.getRightArrow().length());
            drawText(context, canvas, arrowSet.getBottomArrow().getCenterX(), arrowSet.getBottomArrow().getCenterY(), arrowSet.getBottomArrow().length());
    }


    private static void drawText(Context context , Canvas canvas, int x, int y, int distance) {
        if (distance == 0) return;

        int width = Utils.dpToPx(context, 48);
        int height = Utils.dpToPx(context, 24);
        float radius = height / 2f;
        textPath.reset();
        textPath.moveTo(x + radius, y);
        textPath.lineTo(x + width - radius, y);
        textPath.arcTo(new RectF(x + width - 2 * radius, y, x + width, y + 2 * radius), -90, 180);
        textPath.lineTo(x + radius, y + height);
        textPath.arcTo(new RectF(x, y, x + 2 * radius, y + 2 * radius), 90, 180);
        textPath.close();

        float textWidth = textPaint.measureText(distance + " dp");
        float startX = x + (width - textWidth) / 2;
        float startY = y + height / 2 + textPaint.getTextSize() / 2;
        canvas.drawPath(textPath, textBoxPaint);
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
