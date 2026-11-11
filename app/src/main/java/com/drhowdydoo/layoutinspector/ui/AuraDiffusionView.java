package com.drhowdydoo.layoutinspector.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Continuous liquid-ribbon border wave animation using vibrant dynamic wallpaper colors.
 *
 * Instead of discrete circles/balls, this view constructs continuous cubic-spline wave Paths
 * locked to a 5-segment screen-perimeter rail (bottom bezel -> bottom corners -> left/right
 * vertical edges -> top corners -> top-center) and renders three multi-harmonic blurred liquid
 * layers (Ambient Aura, Primary Wave Body, and Specular Bezel Core) into a fast downsampled
 * fluid buffer.
 */
public class AuraDiffusionView extends View {

    // Smooth 4.4-second bottom-to-top fluid border wave duration
    private static final long ANIMATION_DURATION_MS = 3200L;

    // Master border intensity (0.65f = soft glow, 0.88f = vibrant liquid border, 1.0f = max)
    private static final float MASTER_OPACITY = 0.88f;

    // Downsample factor for the offscreen Gaussian fluid buffer (1/8th scale = 64x fewer pixels,
    // enabling true wide-radius Gaussian blur at 120fps with zero discrete "balls")
    private static final int BUFFER_SCALE = 8;

    // Number of spline sample points along each continuous liquid wave ribbon
    private static final int WAVE_SAMPLES = 40;

    private static final int COLOR_COUNT = 4;

    private final Paint bitmapPresentPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint ambientLayerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint primaryWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint specularCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Path leftAmbientPath = new Path();
    private final Path rightAmbientPath = new Path();
    private final Path leftPrimaryPath = new Path();
    private final Path rightPrimaryPath = new Path();
    private final Path leftCorePath = new Path();
    private final Path rightCorePath = new Path();

    private final float[] outerX = new float[WAVE_SAMPLES];
    private final float[] outerY = new float[WAVE_SAMPLES];
    private final float[] innerX = new float[WAVE_SAMPLES];
    private final float[] innerY = new float[WAVE_SAMPLES];
    private final float[] railPoint = new float[4]; // [bezelX, bezelY, inwardNormalX, inwardNormalY]

    private final Matrix gradientMatrix = new Matrix();
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    private Bitmap fluidBitmap;
    private Canvas fluidCanvas;
    private LinearGradient vibrantFlowShader;
    private LinearGradient coreHighlightShader;

    private ValueAnimator auraAnimator;
    private float progress = 0f;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private int bufWidth = 0;
    private int bufHeight = 0;
    private boolean useDynamicColors = true;

    // Fallback vibrant palette if dynamic wallpaper colors are unavailable
    private final int[] vibrantPalette = new int[]{
            0x00E5FF, // Vibrant Cyan
            0x2979FF, // Electric Blue
            0x7C4DFF, // Vibrant Violet
            0x00F5D4  // Luminous Aqua Mint
    };

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

        ambientLayerPaint.setStyle(Paint.Style.FILL);
        primaryWavePaint.setStyle(Paint.Style.FILL);
        specularCorePaint.setStyle(Paint.Style.FILL);
        // Screen blending on the inner core creates a luminous liquid-glass rim right on the bezel
        specularCorePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        updateVibrantDynamicColors();
    }

    /**
     * Extracts dynamic wallpaper colors (Material You on Android 12+, WallpaperColors on 8.1+)
     * and boosts their saturation and brightness into a vibrant palette.
     */
    public void updateVibrantDynamicColors() {
        if (!useDynamicColors) return;
        Context context = getContext();
        if (context == null) return;

        Integer primaryColor = null;
        Integer secondaryColor = null;
        Integer tertiaryColor = null;

        // 1. Android 12+ (API 31+): Material You dynamic tonal values
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                primaryColor = ContextCompat.getColor(context, android.R.color.system_accent1_400);
                secondaryColor = ContextCompat.getColor(context, android.R.color.system_accent2_400);
                tertiaryColor = ContextCompat.getColor(context, android.R.color.system_accent3_400);
            } catch (Exception ignored) {
            }
        }

        // 2. Android 8.1 - 11 (API 27 - 30): WallpaperManager extraction
        if (primaryColor == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                WallpaperColors wc = wm != null
                        ? wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                        : null;
                if (wc != null) {
                    primaryColor = wc.getPrimaryColor().toArgb();
                    secondaryColor = wc.getSecondaryColor() != null
                            ? wc.getSecondaryColor().toArgb()
                            : primaryColor;
                    tertiaryColor = wc.getTertiaryColor() != null
                            ? wc.getTertiaryColor().toArgb()
                            : primaryColor;
                }
            } catch (Exception ignored) {
            }
        }

        if (primaryColor != null) {
            applyVibrantVariantPalette(
                    primaryColor,
                    secondaryColor != null ? secondaryColor : primaryColor,
                    tertiaryColor != null ? tertiaryColor : primaryColor
            );
        }
    }

    private void applyVibrantVariantPalette(int primary, int secondary, int tertiary) {
        float[] hsv1 = new float[3];
        float[] hsv2 = new float[3];
        float[] hsv3 = new float[3];
        Color.colorToHSV(primary, hsv1);
        Color.colorToHSV(secondary, hsv2);
        Color.colorToHSV(tertiary, hsv3);

        // Ensure rich multi-tone hue separation even if the wallpaper uses a monochrome tonal spot
        float hue1 = hsv1[0];
        float hue3 = angularDistance(hue1, hsv3[0]) < 24f ? (hue1 + 45f) % 360f : hsv3[0];
        float hue2 = angularDistance(hue1, hsv2[0]) < 16f ? (hue1 + 22f) % 360f : hsv2[0];
        float hue4 = (hue1 - 28f + 360f) % 360f;

        boolean changed = false;
        int[] newColors = new int[]{
                toVibrantRgb(hue1, hsv1[1], hsv1[2]),
                toVibrantRgb(hue2, hsv2[1], hsv2[2]),
                toVibrantRgb(hue3, hsv3[1], hsv3[2]),
                toVibrantRgb(hue4, hsv1[1], hsv1[2])
        };

        for (int i = 0; i < COLOR_COUNT; i++) {
            if (vibrantPalette[i] != newColors[i]) {
                vibrantPalette[i] = newColors[i];
                changed = true;
            }
        }

        if (changed && bufWidth > 0 && bufHeight > 0) {
            createShadersAndBuffers();
        }
    }

    private float angularDistance(float a, float b) {
        float diff = Math.abs(a - b) % 360f;
        return diff > 180f ? 360f - diff : diff;
    }

    private int toVibrantRgb(float hue, float sat, float val) {
        float vibrantSat = sat < 0.08f ? 0.86f : Math.max(0.84f, Math.min(0.98f, sat * 1.42f));
        float vibrantVal = Math.max(0.94f, Math.min(1.00f, val * 1.22f));
        return Color.HSVToColor(new float[]{hue, vibrantSat, vibrantVal}) & 0x00FFFFFF;
    }

    /**
     * Brightens and slightly desaturates a vibrant color to create a luminous liquid highlight core.
     */
    private int toLuminousCoreColor(int rgb) {
        float[] hsv = new float[3];
        Color.colorToHSV(rgb | 0xFF000000, hsv);
        hsv[1] = Math.max(0.35f, hsv[1] * 0.62f);
        hsv[2] = 1.0f;
        return Color.HSVToColor(255, hsv);
    }

    public void setAuraColors(@NonNull int[] colors) {
        if (colors.length == 0) return;
        this.useDynamicColors = false;
        for (int i = 0; i < COLOR_COUNT; i++) {
            float[] hsv = new float[3];
            Color.colorToHSV(colors[i % colors.length], hsv);
            vibrantPalette[i] = toVibrantRgb(hsv[0], hsv[1], hsv[2]);
        }
        if (bufWidth > 0 && bufHeight > 0) {
            createShadersAndBuffers();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        if (w > 0 && h > 0) {
            this.bufWidth = Math.max(64, w / BUFFER_SCALE);
            this.bufHeight = Math.max(128, h / BUFFER_SCALE);
            srcRect.set(0, 0, bufWidth, bufHeight);
            dstRect.set(0, 0, w, h);
            createShadersAndBuffers();
        }
    }

    private void createShadersAndBuffers() {
        if (fluidBitmap == null || fluidBitmap.getWidth() != bufWidth || fluidBitmap.getHeight() != bufHeight) {
            if (fluidBitmap != null) {
                fluidBitmap.recycle();
            }
            fluidBitmap = Bitmap.createBitmap(bufWidth, bufHeight, Bitmap.Config.ARGB_8888);
            fluidCanvas = new Canvas(fluidBitmap);
        }

        // Configure 3 Gaussian blur radii in buffer space (multiplied by 8x when drawn to screen):
        // - Ambient Aura: ~14px buffer blur -> ~112px silky screen diffusion
        // - Primary Wave: ~6.5px buffer blur -> ~52px smooth liquid body
        // - Specular Core: ~2.5px buffer blur -> ~20px crisp glowing bezel rim
        float minDim = Math.min(bufWidth, bufHeight);
        float ambientBlur = Math.max(6f, minDim * 0.11f);
        float primaryBlur = Math.max(3f, minDim * 0.052f);
        float coreBlur = Math.max(1.5f, minDim * 0.020f);

        ambientLayerPaint.setMaskFilter(new BlurMaskFilter(ambientBlur, BlurMaskFilter.Blur.NORMAL));
        primaryWavePaint.setMaskFilter(new BlurMaskFilter(primaryBlur, BlurMaskFilter.Blur.NORMAL));
        specularCorePaint.setMaskFilter(new BlurMaskFilter(coreBlur, BlurMaskFilter.Blur.NORMAL));

        int c0 = vibrantPalette[0] | 0xFF000000;
        int c1 = vibrantPalette[1] | 0xFF000000;
        int c2 = vibrantPalette[2] | 0xFF000000;
        int c3 = vibrantPalette[3] | 0xFF000000;

        // Repeating 5-stop gradient so the colors can flow continuously upward inside the wave
        vibrantFlowShader = new LinearGradient(
                0f, bufHeight,
                bufWidth * 0.35f, 0f,
                new int[]{c0, c1, c2, c3, c0},
                new float[]{0.0f, 0.28f, 0.55f, 0.80f, 1.0f},
                Shader.TileMode.MIRROR
        );
        ambientLayerPaint.setShader(vibrantFlowShader);
        primaryWavePaint.setShader(vibrantFlowShader);

        coreHighlightShader = new LinearGradient(
                0f, bufHeight,
                0f, 0f,
                new int[]{
                        toLuminousCoreColor(c0),
                        toLuminousCoreColor(c1),
                        toLuminousCoreColor(c2),
                        toLuminousCoreColor(c3)
                },
                new float[]{0.0f, 0.35f, 0.70f, 1.0f},
                Shader.TileMode.MIRROR
        );
        specularCorePaint.setShader(coreHighlightShader);
    }

    public void startAuraAnimation() {
        if (auraAnimator != null && auraAnimator.isRunning()) {
            auraAnimator.cancel();
        }

        updateVibrantDynamicColors();
        setVisibility(VISIBLE);
        progress = 0f;
        invalidate();

        auraAnimator = ValueAnimator.ofFloat(0f, 1f);
        auraAnimator.setDuration(ANIMATION_DURATION_MS);
        // Immediate upward surge from the bottom bezel that glides smoothly to the top-center
        auraAnimator.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.24f, 1.0f));
        auraAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        auraAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(INVISIBLE);
            }
        });
        auraAnimator.start();
    }

    /**
     * Exact 5-segment arc-length parameterized screen perimeter rail in buffer coordinates.
     * Starts right at the bottom-most end of the screen (s = 0.0 at bottom-center, y = bufHeight)
     * and hugs the physical edge all the way to the top-center (s = 1.0 at x = bufWidth/2, y = 0):
     *   1. Bottom horizontal bezel (y = bufHeight) from bottom-center to bottom corner.
     *   2. Bottom 90-degree screen corner arc.
     *   3. Left (x = 0) or Right (x = bufWidth) vertical edge from bottom to top.
     *   4. Top 90-degree screen corner arc.
     *   5. Top horizontal bezel (y = 0) from top corner to exact top-center (x = bufWidth/2, y = 0).
     *
     * @param side -1 for Left Border Rail, +1 for Right Border Rail
     * @param s    normalized arc-length position along the rail [0.0 .. 1.0]
     * @param out  [bezelX, bezelY, inwardNormalX, inwardNormalY]
     */
    private void computeRailPoint(int side, float s, float[] out) {
        float clampedS = Math.max(0f, Math.min(1f, s));
        float halfW = bufWidth * 0.5f;
        float cornerR = Math.min(bufWidth, bufHeight) * 0.065f;

        float bottomHorizLen = Math.max(1f, halfW - cornerR);
        float bottomCornerLen = (float) (Math.PI * 0.5 * cornerR);
        float verticalLen = Math.max(1f, bufHeight - 2f * cornerR);
        float topCornerLen = (float) (Math.PI * 0.5 * cornerR);
        float topHorizLen = Math.max(1f, halfW - cornerR);

        float totalLen = bottomHorizLen + bottomCornerLen + verticalLen + topCornerLen + topHorizLen;
        float d = clampedS * totalLen;

        if (d <= bottomHorizLen) {
            // 1. Bottom-most horizontal edge (y = bufHeight), sweeping outward toward bottom corners
            out[0] = halfW + side * d;
            out[1] = bufHeight;
            out[2] = 0f;
            out[3] = -1f; // Inward normal points straight up from the bottom edge
            return;
        }
        d -= bottomHorizLen;

        if (d <= bottomCornerLen) {
            // 2. Bottom corner 90-degree turn
            float u = d / bottomCornerLen;
            float theta = (float) (u * (Math.PI * 0.5));
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            float cx = (side < 0) ? cornerR : (bufWidth - cornerR);
            float cy = bufHeight - cornerR;

            out[0] = cx + side * cornerR * sinT;
            out[1] = cy + cornerR * cosT;
            out[2] = -side * sinT;
            out[3] = -cosT;
            return;
        }
        d -= bottomCornerLen;

        if (d <= verticalLen) {
            // 3. Left (x = 0) or Right (x = bufWidth) vertical border
            out[0] = (side < 0) ? 0f : bufWidth;
            out[1] = (bufHeight - cornerR) - d;
            out[2] = -side; // Inward normal points horizontally toward screen center
            out[3] = 0f;
            return;
        }
        d -= verticalLen;

        if (d <= topCornerLen) {
            // 4. Top corner 90-degree turn
            float u = d / topCornerLen;
            float theta = (float) (u * (Math.PI * 0.5));
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            float cx = (side < 0) ? cornerR : (bufWidth - cornerR);
            float cy = cornerR;

            out[0] = cx + side * cornerR * cosT;
            out[1] = cy - cornerR * sinT;
            out[2] = -side * cosT;
            out[3] = sinT;
            return;
        }
        d -= topCornerLen;

        // 5. Top horizontal bezel (y = 0) converging to exact Top-Center (halfW, 0)
        float startTopX = (side < 0) ? cornerR : (bufWidth - cornerR);
        out[0] = startTopX - side * d;
        out[1] = 0f;
        out[2] = 0f;
        out[3] = 1f; // Inward normal points straight down from the top edge
    }

    /**
     * Builds a single continuous, smooth cubic/quadratic spline wave ribbon Path along the rail
     * between [tailS, headS]. The outer edge extends past the bezel by outwardBleed so the
     * Gaussian blur stays at 100% strength right on the screen edge, while the inner edge
     * undulates smoothly like a real liquid wave.
     */
    private void buildContinuousWavePath(
            @NonNull Path outPath,
            int side,
            float tailS,
            float headS,
            float thicknessScale,
            float phaseOffset,
            float outwardBleed
    ) {
        outPath.reset();
        float clampedTail = Math.max(0f, Math.min(1f, tailS));
        float clampedHead = Math.max(0f, Math.min(1f, headS));
        float span = clampedHead - clampedTail;
        if (span <= 0.005f) {
            return;
        }

        float sidePhase = (side < 0) ? phaseOffset : (phaseOffset + 0.75f);

        for (int i = 0; i < WAVE_SAMPLES; i++) {
            // u = 0 at trailing tail, u = 1 at leading wave crest
            float u = (float) i / (WAVE_SAMPLES - 1);
            float s = clampedTail + u * span;

            computeRailPoint(side, s, railPoint);
            float bx = railPoint[0];
            float by = railPoint[1];
            float nx = railPoint[2];
            float ny = railPoint[3];

            // Asymmetric fluid droplet envelope: fuller, steeper surging front crest and tapered tail
            float waveEnvelope = (float) (Math.sin(u * Math.PI) * Math.pow(u, 0.32));

            // Starts higher/deeper at the bottom-most end (s = 0) and tapers lower/sleeker
            // as it climbs up the sides and converges at the top-center (s = 1.0)
            float bottomToTopTaper = (float) Math.pow(1.0f - 0.80f * s, 1.15);

            // 3 traveling harmonic sine waves (dispersion) create a continuous, rolling liquid surface
            // with zero discrete "balls"
            float w1 = (float) Math.sin((u * 2.2f - progress * 4.0f) * Math.PI * 2.0 + sidePhase);
            float w2 = (float) Math.sin((s * 4.5f - progress * 6.5f) * Math.PI * 2.0 - sidePhase * 0.7f);
            float w3 = (float) Math.cos((u * 3.8f + progress * 5.2f) * Math.PI * 2.0 + sidePhase * 1.3f);
            float harmonicUndulation = 1.0f + (0.28f * w1) + (0.18f * w2) + (0.10f * w3);

            // Base inward reach at the bottom is ~23% of screen width, tapering smoothly toward top-center
            float inwardReach = bufWidth * 0.23f * thicknessScale
                    * bottomToTopTaper * waveEnvelope * harmonicUndulation;

            // Anchor outer boundary slightly outside the bezel so the blur is 100% solid at the edge
            outerX[i] = bx - nx * outwardBleed;
            outerY[i] = by - ny * outwardBleed;

            // Smooth undulating inner liquid boundary
            innerX[i] = bx + nx * inwardReach;
            innerY[i] = by + ny * inwardReach;
        }

        // Trace the inner undulating liquid wave from tail (0) to head (WAVE_SAMPLES - 1)
        // using continuous midpoint quadratic Bezier splines (C1 smooth, zero sharp corners or balls)
        outPath.moveTo(outerX[0], outerY[0]);
        outPath.lineTo(innerX[0], innerY[0]);

        for (int i = 1; i < WAVE_SAMPLES - 1; i++) {
            float midX = (innerX[i] + innerX[i + 1]) * 0.5f;
            float midY = (innerY[i] + innerY[i + 1]) * 0.5f;
            outPath.quadTo(innerX[i], innerY[i], midX, midY);
        }
        outPath.lineTo(innerX[WAVE_SAMPLES - 1], innerY[WAVE_SAMPLES - 1]);
        outPath.lineTo(outerX[WAVE_SAMPLES - 1], outerY[WAVE_SAMPLES - 1]);

        // Close back along the outer bezel rail from head down to tail
        for (int i = WAVE_SAMPLES - 2; i >= 0; i--) {
            outPath.lineTo(outerX[i], outerY[i]);
        }
        outPath.close();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (viewWidth == 0 || viewHeight == 0 || fluidBitmap == null || vibrantFlowShader == null) {
            return;
        }

        // Clear the low-res fluid buffer
        fluidCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // At progress = 0, the wave already spans [0.00 .. 0.22] right at the bottom-most end
        // (cradling the bottom bezel and bottom corners) and immediately surges up both sides.
        // At progress -> 1, the tail reaches 1.00 (top-center) and dissolves seamlessly.
        float headS = 0.22f + 1.08f * progress;
        float tailS = -0.34f + 1.34f * (float) Math.pow(progress, 1.08);

        // Smooth global fade-in at launch and gentle dissipation as the streams kiss at top-center
        float masterFade;
        if (progress < 0.05f) {
            masterFade = 0.60f + 0.40f * (progress / 0.05f);
        } else if (progress > 0.78f) {
            float t = 1f - ((progress - 0.78f) / 0.22f);
            masterFade = t * t * (3f - 2f * t);
        } else {
            masterFade = 1f;
        }

        int baseAlpha = Math.max(0, Math.min(255, Math.round(masterFade * MASTER_OPACITY * 255f)));
        if (baseAlpha == 0) {
            return;
        }

        // Continuously flow the vibrant gradient upward along with the rising liquid
        gradientMatrix.reset();
        gradientMatrix.postTranslate(0f, -progress * bufHeight * 0.85f);
        vibrantFlowShader.setLocalMatrix(gradientMatrix);
        coreHighlightShader.setLocalMatrix(gradientMatrix);

        float bleed = Math.max(4f, bufWidth * 0.08f);

        // Build continuous spline wave ribbons for Left (-1) and Right (+1) borders:
        // 1. Ambient diffused aura ribbon (widest inward reach, softest Gaussian diffusion)
        buildContinuousWavePath(leftAmbientPath, -1, tailS - 0.03f, headS + 0.02f, 1.25f, 0.0f, bleed);
        buildContinuousWavePath(rightAmbientPath, 1, tailS - 0.03f, headS + 0.02f, 1.25f, 0.0f, bleed);

        // 2. Primary vibrant liquid wave body (rich undulating crest & trough silhouette)
        buildContinuousWavePath(leftPrimaryPath, -1, tailS, headS, 0.92f, 1.4f, bleed);
        buildContinuousWavePath(rightPrimaryPath, 1, tailS, headS, 0.92f, 1.4f, bleed);

        // 3. Luminous bezel core (sleek inner highlight hugging the screen edge)
        buildContinuousWavePath(leftCorePath, -1, tailS + 0.03f, headS - 0.01f, 0.48f, 2.8f, bleed);
        buildContinuousWavePath(rightCorePath, 1, tailS + 0.03f, headS - 0.01f, 0.48f, 2.8f, bleed);

        ambientLayerPaint.setAlpha(Math.round(baseAlpha * 0.58f));
        primaryWavePaint.setAlpha(Math.round(baseAlpha * 0.88f));
        specularCorePaint.setAlpha(Math.round(baseAlpha * 0.78f));

        // Render the 3 continuous liquid layers into the Gaussian fluid buffer
        fluidCanvas.drawPath(leftAmbientPath, ambientLayerPaint);
        fluidCanvas.drawPath(rightAmbientPath, ambientLayerPaint);

        fluidCanvas.drawPath(leftPrimaryPath, primaryWavePaint);
        fluidCanvas.drawPath(rightPrimaryPath, primaryWavePaint);

        fluidCanvas.drawPath(leftCorePath, specularCorePaint);
        fluidCanvas.drawPath(rightCorePath, specularCorePaint);

        // Present the hardware-filtered fluid buffer onto the screen
        canvas.drawBitmap(fluidBitmap, srcRect, dstRect, bitmapPresentPaint);
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