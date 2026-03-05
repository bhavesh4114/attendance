package com.example.majuri_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Draws a fixed face guide oval and status hint for camera alignment.
 */
public class FaceGuideOverlayView extends View {

    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF guideRect = new RectF();
    private final RectF progressRect = new RectF();

    private boolean faceDetected = false;
    private boolean aligned = false;
    private int stableFrames = 0;
    private int stableTarget = 8;
    private String hint = "Align your face in the frame";

    public FaceGuideOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceGuideOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceGuideOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(dp(3f));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14f));

        progressBgPaint.setColor(Color.parseColor("#44FFFFFF"));
        progressBgPaint.setStyle(Paint.Style.FILL);

        progressPaint.setColor(Color.parseColor("#22C55E"));
        progressPaint.setStyle(Paint.Style.FILL);
    }

    public void updateState(boolean faceDetected, boolean aligned, int stableFrames, int stableTarget, String hint) {
        this.faceDetected = faceDetected;
        this.aligned = aligned;
        this.stableFrames = Math.max(0, stableFrames);
        this.stableTarget = Math.max(1, stableTarget);
        this.hint = hint != null ? hint : "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float ovalW = w * 0.62f;
        float ovalH = h * 0.52f;
        float left = (w - ovalW) / 2f;
        float top = (h - ovalH) / 2f - dp(18f);
        guideRect.set(left, top, left + ovalW, top + ovalH);

        if (!faceDetected) {
            guidePaint.setColor(Color.parseColor("#EF4444"));
        } else if (aligned) {
            guidePaint.setColor(Color.parseColor("#22C55E"));
        } else {
            guidePaint.setColor(Color.parseColor("#F59E0B"));
        }
        canvas.drawOval(guideRect, guidePaint);

        float progressLeft = left;
        float progressTop = guideRect.bottom + dp(20f);
        float progressRight = left + ovalW;
        float progressBottom = progressTop + dp(8f);
        progressRect.set(progressLeft, progressTop, progressRight, progressBottom);
        canvas.drawRoundRect(progressRect, dp(4f), dp(4f), progressBgPaint);

        float ratio = Math.min(1f, stableFrames / (float) stableTarget);
        RectF fillRect = new RectF(progressRect.left, progressRect.top, progressRect.left + (progressRect.width() * ratio), progressRect.bottom);
        canvas.drawRoundRect(fillRect, dp(4f), dp(4f), progressPaint);

        float hintY = progressBottom + dp(24f);
        canvas.drawText(hint, left, hintY, textPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
