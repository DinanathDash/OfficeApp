package com.dinanathdash.officeapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DonutProgressView extends View {

    private Paint backgroundPaint;
    private Paint videoPaint;
    private Paint imagePaint;
    private Paint docPaint;
    private RectF rectF;

    private float videoPercentage = 0;
    private float imagePercentage = 0;
    private float docPercentage = 0;
    private final float strokeWidth = 40f; 

    public DonutProgressView(Context context) {
        super(context);
        init();
    }

    public DonutProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setColor(0x33FFFFFF); // Translucent white for empty part
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        videoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        videoPaint.setStyle(Paint.Style.STROKE);
        videoPaint.setStrokeWidth(strokeWidth);
        videoPaint.setColor(0xFF0055FF); // Check colors from Reference if possible (Blue)
        videoPaint.setStrokeCap(Paint.Cap.ROUND);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imagePaint.setStyle(Paint.Style.STROKE);
        imagePaint.setStrokeWidth(strokeWidth);
        imagePaint.setColor(0xFF448AFF); // Lighter Blue
        imagePaint.setStrokeCap(Paint.Cap.ROUND);

        docPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        docPaint.setStyle(Paint.Style.STROKE);
        docPaint.setStrokeWidth(strokeWidth);
        docPaint.setColor(0xFFBBDEFB); // Very Light Blue
        docPaint.setStrokeCap(Paint.Cap.ROUND);

        rectF = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth / 2;
        rectF.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background circle
        canvas.drawArc(rectF, 0, 360, false, backgroundPaint);

        float startAngle = -90; // Start from top

        // Draw Video Arc
        float videoSweep = (videoPercentage / 100f) * 360;
        if (videoSweep > 0) {
            canvas.drawArc(rectF, startAngle, videoSweep, false, videoPaint);
            startAngle += videoSweep;
        }

        // Draw Image Arc
        float imageSweep = (imagePercentage / 100f) * 360;
        if (imageSweep > 0) {
            canvas.drawArc(rectF, startAngle, imageSweep, false, imagePaint);
            startAngle += imageSweep;
        }

        // Draw Doc Arc
        float docSweep = (docPercentage / 100f) * 360;
        if (docSweep > 0) {
            canvas.drawArc(rectF, startAngle, docSweep, false, docPaint);
            startAngle += docSweep;
        }
    }

    public void setPercentages(float video, float image, float doc) {
        this.videoPercentage = video;
        this.imagePercentage = image;
        this.docPercentage = doc;
        invalidate();
    }
}
