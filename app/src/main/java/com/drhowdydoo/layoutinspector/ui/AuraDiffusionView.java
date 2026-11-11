package com.drhowdydoo.layoutinspector.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class AuraDiffusionView extends View {

    // 4.6-second smooth upward flow with zero startup lag
    private static final long ANIMATION_DURATION_MS = 4600L;

    // Master translucency control (0.25f = whisper-faint, 0.38f = subtle wash, 0.55f = richer tint)
    private static final float MASTER_OPACITY = 0.38f;

    private static final int PLUME_COUNT = 7;

    private final Paint[] plumePaints = new Paint[PLUME_COUNT];
    private final Matrix[] plumeMatrices = new Matrix[PLUME_COUNT];
    private final RadialGradient[] plumeShaders = new RadialGradient[PLUME_COUNT];
    private final FluidPlume[] plumes = new FluidPlume[PLUME_COUNT];

    private ValueAnimator auraAnimator;
    private float progress = 0f;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private boolean useWallpaperColors = true;

    private final int[] basePalette = new int[]{
            0x64D8E8, // Soft Mist Cyan
            0x7E78D2, // Muted Lavender Indigo
            0x6E85E8, // Airy Periwinkle
            0x5CC8D8, // Pale Aqua Veil
            0xB86BD6, // Soft Orchid Mist
            0x8E74E0, // Muted Wisteria
            0x7ADCE6  // Pale Sky Tint
    };

    private static class FluidPlume {
        final float startTime;
        final float endTime;
        final float baseXRatio;
        final float swayAmpRatio;
        final float swayFreq;
        final float swayPhase;
        final float radiusXRatio;
        final float radiusYRatio;
        final float startAngle;
        final float endAngle;
        final float weight;

        FluidPlume(float startTime, float endTime,
                   float baseXRatio, float swayAmpRatio, float swayFreq, float swayPhase,
                   float radiusXRatio, float radiusYRatio,
                   float startAngle, float endAngle, float weight) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.baseXRatio = baseXRatio;
            this.swayAmpRatio = swayAmpRatio;
            this.swayFreq = swayFreq;
            this.swayPhase = swayPhase;
            this.radiusXRatio = radiusXRatio;
            this.radiusYRatio = radiusYRatio;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.weight = weight;
        }
    }

    public AuraDiffusionView(Context context) {
        super(context);
        init();
    }

    public AuraDiffusionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuraDiffusionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);

        for (int i = 0; i < PLUME_COUNT; i++) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            plumePaints[i] = paint;
            plumeMatrices[i] = new Matrix();
        }

        // Tighter start stagger (0.00f to 0.06f) so plumes begin rising immediately on frame 1
        plumes[0] = new FluidPlume(
                0.00f, 0.94f,
                0.30f, 0.20f, 1.1f, 0.0f,
                1.20f, 0.75f, -16f, 20f, 0.22f);

        plumes[1] = new FluidPlume(
                0.00f, 0.96f,
                0.70f, -0.20f, 1.0f, 0.9f,
                1.20f, 0.78f, 18f, -16f, 0.22f);

        plumes[2] = new FluidPlume(
                0.00f, 0.95f,
                0.50f, 0.14f, 0.85f, 1.7f,
                1.45f, 0.85f, -8f, 12f, 0.20f);

        plumes[3] = new FluidPlume(
                0.02f, 0.98f,
                0.15f, 0.24f, 0.95f, 0.5f,
                1.15f, 0.80f, 14f, -22f, 0.18f);

        plumes[4] = new FluidPlume(
                0.03f, 1.00f,
                0.85f, -0.24f, 0.95f, 2.2f,
                1.15f, 0.80f, -14f, 22f, 0.18f);

        plumes[5] = new FluidPlume(
                0.05f, 1.00f,
                0.38f, -0.18f, 1.2f, 2.9f,
                1.30f, 0.88f, 22f, -10f, 0.17f);

        plumes[6] = new FluidPlume(
                0.06f, 1.00f,
                0.62f, 0.18f, 1.15f, 3.6f,
                1.30f, 0.90f, -20f, 14f, 0.16f);

        updateWallpaperColors();
    }

    public void updateWallpaperColors() {
        if (!useWallpaperColors) return;
        Context context = getContext();
        if (context == null) return;

        boolean changed = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                int[] dynamicColors = new int[]{
                        ContextCompat.getColor(context, android.R.color.system_accent1_200),
                        ContextCompat.getColor(context, android.R.color.system_accent2_200),
                        ContextCompat.getColor(context, android.R.color.system_accent3_200),
                        ContextCompat.getColor(context, android.R.color.system_accent1_300),
                        ContextCompat.getColor(context, android.R.color.system_accent3_300),
                        ContextCompat.getColor(context, android.R.color.system_accent2_300),
                        ContextCompat.getColor(context, android.R.color.system_accent1_100)
                };
                for (int i = 0; i < PLUME_COUNT; i++) {
                    int softened = softenForAura(dynamicColors[i], i * 4f);
                    if (basePalette[i] != softened) {
                        basePalette[i] = softened;
                        changed = true;
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                WallpaperColors wc = wm != null
                        ? wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                        : null;
                if (wc != null) {
                    int primary = wc.getPrimaryColor().toArgb();
                    int secondary = wc.getSecondaryColor() != null
                            ? wc.getSecondaryColor().toArgb()
                            : shiftHue(primary, 28f);
                    int tertiary = wc.getTertiaryColor() != null
                            ? wc.getTertiaryColor().toArgb()
                            : shiftHue(primary, -32f);

                    int[] extracted = new int[]{
                            primary,
                            secondary,
                            tertiary,
                            shiftHue(primary, 15f),
                            shiftHue(tertiary, -15f),
                            shiftHue(secondary, 18f),
                            shiftHue(primary, -20f)
                    };
                    for (int i = 0; i < PLUME_COUNT; i++) {
                        int softened = softenForAura(extracted[i], 0f);
                        if (basePalette[i] != softened) {
                            basePalette[i] = softened;
                            changed = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (changed && viewWidth > 0 && viewHeight > 0) {
            createShaders();
        }
    }

    private int softenForAura(int color, float hueOffsetDegrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + hueOffsetDegrees + 360f) % 360f;
        if (hsv[1] > 0.05f) {
            hsv[1] = Math.max(0.28f, Math.min(0.55f, hsv[1] * 0.85f));
        }
        hsv[2] = Math.max(0.78f, Math.min(0.92f, hsv[2]));
        return Color.HSVToColor(hsv) & 0x00FFFFFF;
    }

    private int shiftHue(int color, float degrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + degrees + 360f) % 360f;
        return Color.HSVToColor(hsv);
    }

    public void setAuraColors(@NonNull int[] colors) {
        if (colors.length == 0) return;
        this.useWallpaperColors = false;
        for (int i = 0; i < PLUME_COUNT; i++) {
            basePalette[i] = softenForAura(colors[i % colors.length], 0f);
        }
        if (viewWidth > 0 && viewHeight > 0) {
            createShaders();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        if (w > 0 && h > 0) {
            createShaders();
        }
    }

    private void createShaders() {
        float[] stops = new float[]{0.0f, 0.16f, 0.34f, 0.52f, 0.70f, 0.86f, 1.0f};
        float[] falloff = new float[]{1.0f, 0.86f, 0.62f, 0.36f, 0.16f, 0.04f, 0.0f};
        int shaderInternalPeakAlpha = 180;

        for (int i = 0; i < PLUME_COUNT; i++) {
            int rgb = basePalette[i] & 0x00FFFFFF;
            int r = Color.red(rgb);
            int g = Color.green(rgb);
            int b = Color.blue(rgb);

            int[] gradientColors = new int[stops.length];
            for (int s = 0; s < stops.length; s++) {
                int a = Math.round(shaderInternalPeakAlpha * falloff[s]);
                gradientColors[s] = Color.argb(a, r, g, b);
            }

            plumeShaders[i] = new RadialGradient(
                    0f, 0f, 1f,
                    gradientColors,
                    stops,
                    Shader.TileMode.CLAMP
            );
            plumePaints[i].setShader(plumeShaders[i]);
        }
    }

    public void startAuraAnimation() {
        if (auraAnimator != null && auraAnimator.isRunning()) {
            auraAnimator.cancel();
        }

        updateWallpaperColors();
        setVisibility(VISIBLE);
        progress = 0f;
        invalidate(); // Draw frame 0 immediately without waiting for next choreographer tick

        auraAnimator = ValueAnimator.ofFloat(0f, 1f);
        auraAnimator.setDuration(ANIMATION_DURATION_MS);
        // Starts with immediate upward velocity at t=0 (no ease-in pause) and glides smoothly to the top
        auraAnimator.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.25f, 1.0f));
        auraAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        auraAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Keep INVISIBLE instead of GONE so dimensions remain cached for next invocation
                setVisibility(INVISIBLE);
            }
        });
        auraAnimator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (viewWidth == 0 || viewHeight == 0 || plumeShaders[0] == null) {
            return;
        }

        for (int i = 0; i < PLUME_COUNT; i++) {
            FluidPlume p = plumes[i];
            if (progress < p.startTime) {
                continue;
            }

            // Linear localT (since PathInterpolator already shapes the global curve,
            // avoiding double-easing lag at the start)
            float localT = Math.min(1f, (progress - p.startTime) / (p.endTime - p.startTime));

            float diffusionSpread = 1.0f + (0.55f * localT);
            float radiusX = viewWidth * p.radiusXRatio * diffusionSpread;
            float radiusY = viewHeight * p.radiusYRatio * (1.0f + 0.45f * localT);

            // Starts closer to the bottom screen edge so the soft upper glow is visible on frame 1
            float startY = viewHeight + (radiusY * 0.22f);
            float endY = -(radiusY * 0.50f);
            float currentY = startY + (endY - startY) * localT;

            float waveAngle = (float) (localT * p.swayFreq * Math.PI + p.swayPhase);
            float currentX = viewWidth * (p.baseXRatio + p.swayAmpRatio * (float) Math.sin(waveAngle));

            float currentRotation = p.startAngle + (p.endAngle - p.startAngle) * localT;

            // Quick, soft entry fade (starts at 35% strength at t=0 and reaches 100% by localT = 0.08)
            float alphaEnvelope;
            if (localT < 0.08f) {
                alphaEnvelope = 0.35f + 0.65f * (localT / 0.08f);
            } else if (localT > 0.42f) {
                float fadeOut = 1f - ((localT - 0.42f) / 0.58f);
                alphaEnvelope = fadeOut * fadeOut * (3f - 2f * fadeOut);
            } else {
                alphaEnvelope = 1f;
            }

            float combinedAlpha = alphaEnvelope * p.weight * MASTER_OPACITY;
            int paintAlpha = Math.max(0, Math.min(255, Math.round(combinedAlpha * 255f)));
            if (paintAlpha == 0) {
                continue;
            }

            plumePaints[i].setAlpha(paintAlpha);

            Matrix matrix = plumeMatrices[i];
            matrix.reset();
            matrix.postScale(radiusX, radiusY);
            matrix.postRotate(currentRotation);
            matrix.postTranslate(currentX, currentY);
            plumeShaders[i].setLocalMatrix(matrix);

            canvas.drawRect(0f, 0f, viewWidth, viewHeight, plumePaints[i]);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (auraAnimator != null) {
            auraAnimator.cancel();
            auraAnimator = null;
        }
    }
}