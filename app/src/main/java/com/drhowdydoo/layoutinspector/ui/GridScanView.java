package com.drhowdydoo.layoutinspector.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Smooth bottom-to-top white grid scan animation.
 *
 * Renders a two-tier wireframe grid (fine sub-grid + major grid lines), illuminated
 * intersection nodes, and a soft leading horizon glow that sweeps smoothly upward
 * from the bottom edge to the top of the screen.
 */
public class GridScanView extends View {

    // Total duration of the bottom-to-top grid sweep
    private static final long ANIMATION_DURATION_MS = 2600L;

    // Master intensity multiplier (0.0f .. 1.0f)
    private static final float MASTER_OPACITY = 0.90f;

    // Grid cell size in dp (28dp gives a clean, balanced inspector grid)
    private static final float GRID_CELL_SIZE_DP = 28f;

    // Every Nth line is drawn slightly brighter as a major structural line
    private static final int MAJOR_LINE_INTERVAL = 4;

    // Height of the travelling vertical wave band as a fraction of screen height
    private static final float WAVE_BAND_HEIGHT_RATIO = 0.55f;

    // Base animation color (Pure White)
    private static final int GRID_COLOR_RGB = 0x00FFFFFF;

    private final Paint minorGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint horizonGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix shaderMatrix = new Matrix();

    private LinearGradient minorGridShader;
    private LinearGradient majorGridShader;
    private LinearGradient horizonGlowShader;

    private float[] minorLinesBuffer = new float[0];
    private float[] majorLinesBuffer = new float[0];
    private float[] colPositions = new float[0];
    private float[] rowPositions = new float[0];

    private ValueAnimator gridAnimator;
    private float progress = 0f;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private float cellSizePx = 0f;
    private float waveBandHeightPx = 0f;
    private float maxNodeRadiusPx = 0f;

    public GridScanView(Context context) {
        super(context);
        init(context);
    }

    public GridScanView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GridScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClickable(false);
        setFocusable(false);

        float density = context.getResources().getDisplayMetrics().density;
        cellSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                GRID_CELL_SIZE_DP,
                context.getResources().getDisplayMetrics()
        );
        maxNodeRadiusPx = 2.2f * density;

        minorGridPaint.setStyle(Paint.Style.STROKE);
        minorGridPaint.setStrokeWidth(Math.max(1f, 0.85f * density));

        majorGridPaint.setStyle(Paint.Style.STROKE);
        majorGridPaint.setStrokeWidth(Math.max(1.5f, 1.15f * density));

        horizonGlowPaint.setStyle(Paint.Style.FILL);
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setColor(Color.WHITE);
    }

    /**
     * Retained for drop-in compatibility with existing callers.
     */
    public void updateVibrantDynamicColors() {
        // Intentionally kept as a no-op since the grid uses pure white.
    }

    /**
     * Retained for drop-in compatibility with existing callers.
     */
    public void setAuraColors(@NonNull int[] colors) {
        // Intentionally kept as a no-op since the grid uses pure white.
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        if (w > 0 && h > 0) {
            this.waveBandHeightPx = h * WAVE_BAND_HEIGHT_RATIO;
            buildGridGeometry(w, h);
            buildWaveShaders();
        }
    }

    /**
     * Pre-computes all vertical and horizontal grid line segments so onDraw()
     * performs zero allocations and renders the grid in two batched GPU calls.
     */
    private void buildGridGeometry(int w, int h) {
        int colCount = Math.max(1, (int) Math.ceil(w / cellSizePx));
        int rowCount = Math.max(1, (int) Math.ceil(h / cellSizePx));

        // Center the grid horizontally so left and right margins are symmetrical
        float gridSpanW = colCount * cellSizePx;
        float startX = (w - gridSpanW) * 0.5f;

        // Anchor the grid to the bottom edge (y = h) so it rises cleanly from the bottom bezel
        float startY = h - (rowCount * cellSizePx);

        colPositions = new float[colCount + 1];
        rowPositions = new float[rowCount + 1];

        int majorCount = 0;
        int minorCount = 0;

        for (int c = 0; c <= colCount; c++) {
            colPositions[c] = startX + c * cellSizePx;
            if (c % MAJOR_LINE_INTERVAL == 0) {
                majorCount++;
            } else {
                minorCount++;
            }
        }

        for (int r = 0; r <= rowCount; r++) {
            rowPositions[r] = startY + r * cellSizePx;
            if (r % MAJOR_LINE_INTERVAL == 0) {
                majorCount++;
            } else {
                minorCount++;
            }
        }

        majorLinesBuffer = new float[majorCount * 4];
        minorLinesBuffer = new float[minorCount * 4];

        int majorIdx = 0;
        int minorIdx = 0;

        // Vertical lines
        for (int c = 0; c <= colCount; c++) {
            float x = colPositions[c];
            if (c % MAJOR_LINE_INTERVAL == 0) {
                majorLinesBuffer[majorIdx++] = x;
                majorLinesBuffer[majorIdx++] = 0f;
                majorLinesBuffer[majorIdx++] = x;
                majorLinesBuffer[majorIdx++] = h;
            } else {
                minorLinesBuffer[minorIdx++] = x;
                minorLinesBuffer[minorIdx++] = 0f;
                minorLinesBuffer[minorIdx++] = x;
                minorLinesBuffer[minorIdx++] = h;
            }
        }

        // Horizontal lines
        for (int r = 0; r <= rowCount; r++) {
            float y = rowPositions[r];
            if (r % MAJOR_LINE_INTERVAL == 0) {
                majorLinesBuffer[majorIdx++] = 0f;
                majorLinesBuffer[majorIdx++] = y;
                majorLinesBuffer[majorIdx++] = w;
                majorLinesBuffer[majorIdx++] = y;
            } else {
                minorLinesBuffer[minorIdx++] = 0f;
                minorLinesBuffer[minorIdx++] = y;
                minorLinesBuffer[minorIdx++] = w;
                minorLinesBuffer[minorIdx++] = y;
            }
        }
    }

    /**
     * Creates vertical LinearGradient shaders defined in local wave coordinates [0 .. waveBandHeightPx],
     * where y = 0 is the leading top edge of the rising wave and y = waveBandHeightPx is the trailing tail.
     */
    private void buildWaveShaders() {
        float bandH = Math.max(1f, waveBandHeightPx);

        // Minor grid lines: soft white crest (38% peak alpha) with smooth trailing fade
        minorGridShader = new LinearGradient(
                0f, 0f,
                0f, bandH,
                new int[]{
                        withAlpha(GRID_COLOR_RGB, 0),
                        withAlpha(GRID_COLOR_RGB, 96),
                        withAlpha(GRID_COLOR_RGB, 52),
                        withAlpha(GRID_COLOR_RGB, 16),
                        withAlpha(GRID_COLOR_RGB, 0)
                },
                new float[]{0.0f, 0.14f, 0.42f, 0.78f, 1.0f},
                Shader.TileMode.CLAMP
        );
        minorGridPaint.setShader(minorGridShader);

        // Major grid lines: crisper white crest (68% peak alpha)
        majorGridShader = new LinearGradient(
                0f, 0f,
                0f, bandH,
                new int[]{
                        withAlpha(GRID_COLOR_RGB, 0),
                        withAlpha(GRID_COLOR_RGB, 175),
                        withAlpha(GRID_COLOR_RGB, 95),
                        withAlpha(GRID_COLOR_RGB, 28),
                        withAlpha(GRID_COLOR_RGB, 0)
                },
                new float[]{0.0f, 0.14f, 0.45f, 0.80f, 1.0f},
                Shader.TileMode.CLAMP
        );
        majorGridPaint.setShader(majorGridShader);

        // Subtle ambient white glow behind the grid wave crest (16% peak alpha)
        horizonGlowShader = new LinearGradient(
                0f, 0f,
                0f, bandH,
                new int[]{
                        withAlpha(GRID_COLOR_RGB, 0),
                        withAlpha(GRID_COLOR_RGB, 42),
                        withAlpha(GRID_COLOR_RGB, 18),
                        withAlpha(GRID_COLOR_RGB, 0)
                },
                new float[]{0.0f, 0.16f, 0.52f, 1.0f},
                Shader.TileMode.CLAMP
        );
        horizonGlowPaint.setShader(horizonGlowShader);
    }

    private static int withAlpha(int rgb, int alpha) {
        int clampedA = Math.max(0, Math.min(255, alpha));
        return (clampedA << 24) | (rgb & 0x00FFFFFF);
    }

    public void startAuraAnimation() {
        if (gridAnimator != null && gridAnimator.isRunning()) {
            gridAnimator.cancel();
        }

        setVisibility(VISIBLE);
        progress = 0f;
        invalidate();

        gridAnimator = ValueAnimator.ofFloat(0f, 1f);
        gridAnimator.setDuration(ANIMATION_DURATION_MS);
        // Smooth upward deceleration curve
        gridAnimator.setInterpolator(new PathInterpolator(0.18f, 0.0f, 0.22f, 1.0f));
        gridAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        gridAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(INVISIBLE);
            }
        });
        gridAnimator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (viewWidth == 0 || viewHeight == 0 || minorGridShader == null) {
            return;
        }

        // Smooth master fade-in at the bottom and gentle dissolve at the top
        float masterFade;
        if (progress < 0.06f) {
            masterFade = progress / 0.06f;
        } else if (progress > 0.80f) {
            float t = 1f - ((progress - 0.80f) / 0.20f);
            masterFade = t * t * (3f - 2f * t);
        } else {
            masterFade = 1f;
        }

        int paintAlpha = Math.max(0, Math.min(255, Math.round(masterFade * MASTER_OPACITY * 255f)));
        if (paintAlpha == 0) {
            return;
        }

        // Wavefront top (waveTopY) starts just below the bottom bezel (viewHeight)
        // and travels all the way past the top edge (-waveBandHeightPx * 0.65f)
        float totalTravel = viewHeight + waveBandHeightPx * 0.75f;
        float waveTopY = viewHeight - (progress * totalTravel);
        float waveBottomY = waveTopY + waveBandHeightPx;

        // Translate the vertical shaders to match the rising wave band
        shaderMatrix.reset();
        shaderMatrix.setTranslate(0f, waveTopY);
        minorGridShader.setLocalMatrix(shaderMatrix);
        majorGridShader.setLocalMatrix(shaderMatrix);
        horizonGlowShader.setLocalMatrix(shaderMatrix);

        minorGridPaint.setAlpha(paintAlpha);
        majorGridPaint.setAlpha(paintAlpha);
        horizonGlowPaint.setAlpha(paintAlpha);

        // 1. Soft upward horizon glow band
        float clampTop = Math.max(0f, waveTopY);
        float clampBottom = Math.min(viewHeight, waveBottomY);
        if (clampBottom > clampTop) {
            canvas.drawRect(0f, clampTop, viewWidth, clampBottom, horizonGlowPaint);
        }

        // 2. Crisp minor and major wireframe grid lines
        canvas.drawLines(minorLinesBuffer, minorGridPaint);
        canvas.drawLines(majorLinesBuffer, majorGridPaint);

        // 3. Luminous intersection nodes along the rising wavefront crest
        drawWavefrontNodes(canvas, waveTopY, masterFade * MASTER_OPACITY);
    }

    /**
     * Draws glowing white intersection dots inside the active wave band.
     * Applies a subtle parabolic dome offset across the X-axis so the center of the grid
     * crests slightly ahead of the outer edges for an organic upward sweep.
     */
    private void drawWavefrontNodes(@NonNull Canvas canvas, float waveTopY, float masterIntensity) {
        if (rowPositions.length == 0 || colPositions.length == 0) {
            return;
        }

        float halfW = viewWidth * 0.5f;
        float maxArchOffset = cellSizePx * 1.15f;
        float activeNodeBand = waveBandHeightPx * 0.65f;

        for (int r = 0; r < rowPositions.length; r++) {
            float y = rowPositions[r];
            // Quick vertical bounds rejection outside the active node band
            if (y < waveTopY - maxArchOffset || y > waveTopY + activeNodeBand + maxArchOffset) {
                continue;
            }

            boolean isMajorRow = (r % MAJOR_LINE_INTERVAL == 0);

            for (int c = 0; c < colPositions.length; c++) {
                float x = colPositions[c];
                // Normalized horizontal distance from screen center [0.0 at center .. 1.0 at edges]
                float normX = (x - halfW) / Math.max(1f, halfW);
                // Center leads slightly, edges trail slightly for a smooth dome wavefront
                float localWaveTopY = waveTopY + (normX * normX) * maxArchOffset;

                float distFromFront = y - localWaveTopY;
                if (distFromFront <= 0f || distFromFront >= activeNodeBand) {
                    continue;
                }

                // Normalized position inside the node wave [0.0 = leading edge, 1.0 = trailing tail]
                float u = distFromFront / activeNodeBand;

                // Sharp rise at the crest (u ~ 0.18) with a smooth decay behind it
                float envelope = (float) (Math.sin(u * Math.PI) * Math.pow(1f - u, 0.75));
                if (envelope <= 0.02f) {
                    continue;
                }

                boolean isMajorIntersection = isMajorRow && (c % MAJOR_LINE_INTERVAL == 0);
                float alphaScale = isMajorIntersection ? 0.95f : 0.48f;
                float radiusScale = isMajorIntersection ? 1.0f : 0.62f;

                int nodeAlpha = Math.round(255f * masterIntensity * alphaScale * envelope);
                if (nodeAlpha > 4) {
                    nodePaint.setAlpha(nodeAlpha);
                    canvas.drawCircle(x, y, maxNodeRadiusPx * radiusScale * (0.5f + 0.5f * envelope), nodePaint);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (gridAnimator != null) {
            gridAnimator.cancel();
            gridAnimator = null;
        }
    }
}