package com.drhowdydoo.layoutinspector.util;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;

public class DrawableLayoutHelper {

    private static SharedPreferences preferences;
    private static int paintColor;
    private static int selectedColor;
    private static float paintWidth;
    private static boolean showLayoutBounds;
    private static boolean showViewPosition;
    private static int measurementUnit;

    public static void init(Context context){
        preferences = context.getSharedPreferences("com.drhowdydoo.layoutinspector.preferences", MODE_PRIVATE);
        notifyPreferenceChange();
        selectedColor = createSelectedColor(paintColor);
        LayoutBoundsDrawer.init(context, paintColor, selectedColor, paintWidth);
        DistanceArrowDrawer.init(context, selectedColor, paintWidth);
    }

    /** Creates a saturated accent that remains harmonious with the configured bounds color. */
    private static int createSelectedColor(int baseColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);

        // Shift the hue toward cyan/blue while preserving a visible contrast with the base color.
        hsv[0] = (hsv[0] + 68f) % 360f;
        hsv[1] = Math.max(hsv[1], 0.90f);
        hsv[2] = 0.88f;
        return Color.HSVToColor(hsv);
    }

    public static void notifyPreferenceChange(){
        paintColor = preferences.getInt("SETTINGS_STROKE_COLOR", Color.parseColor("#2E7D32"));
        selectedColor = createSelectedColor(paintColor);
        paintWidth = preferences.getFloat("SETTINGS_STROKE_WIDTH", 2.5f);
        showLayoutBounds = preferences.getBoolean("SETTINGS_SHOW_LAYOUT_BOUNDS", false);
        showViewPosition = preferences.getBoolean("SETTINGS_SHOW_VIEW_POSITION", false);
        measurementUnit = preferences.getInt("APP_SETTINGS_UNIT_TYPE", 0);
        DistanceArrowDrawer.notifyMeasurementUnitChange(measurementUnit);
    }

    public static void notifyPaintChange(){
        notifyPreferenceChange();
        LayoutBoundsDrawer.notifyPaintChange(paintColor, selectedColor, paintWidth);
        DistanceArrowDrawer.notifyPaintChange(selectedColor, paintWidth);
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
