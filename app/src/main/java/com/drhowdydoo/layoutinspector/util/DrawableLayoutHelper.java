package com.drhowdydoo.layoutinspector.util;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;

public class DrawableLayoutHelper {

    private static SharedPreferences preferences;
    private static int paintColor;
    private static float paintWidth;
    private static boolean showLayoutBounds;
    private static boolean showViewPosition;
    private static int measurementUnit;

    public static void init(Context context){
        preferences = context.getSharedPreferences("com.drhowdydoo.layoutinspector.preferences", MODE_PRIVATE);
        notifyPreferenceChange();
        LayoutBoundsDrawer.init(context, paintColor, paintWidth);
        DistanceArrowDrawer.init(context, paintColor, paintWidth);
    }

    public static void notifyPreferenceChange(){
        paintColor = preferences.getInt("SETTINGS_STROKE_COLOR", Color.parseColor("#2E7D32"));
        paintWidth = preferences.getFloat("SETTINGS_STROKE_WIDTH", 2.5f);
        showLayoutBounds = preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", false);
        showViewPosition = preferences.getBoolean("SETTINGS_SHOW_VIEW_POSITION", false);
        measurementUnit = preferences.getInt("APP_SETTINGS_UNIT_TYPE", 0);
        DistanceArrowDrawer.notifyMeasurementUnitChange(measurementUnit);
    }

    public static void notifyPaintChange(){
        notifyPreferenceChange();
        LayoutBoundsDrawer.notifyPaintChange(paintColor, paintWidth);
        DistanceArrowDrawer.notifyPaintChange(paintColor, paintWidth);
    }

    public static void onDraw(Canvas canvas, Context context){
        LayoutBoundsDrawer.onDraw(canvas, showLayoutBounds);
        if (showViewPosition) DistanceArrowDrawer.onDraw(canvas, context);
    }

    public static void clearCanvas(){
        LayoutBoundsDrawer.clearCanvas();
        DistanceArrowDrawer.clearCanvas();
    }

}
