package com.dinanathdash.officeapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dinanathdash.officeapp.data.Slide;
import com.dinanathdash.officeapp.data.SlideElement;

public class SlideView extends View {

    private Slide slide;
    private float scaleFactor = 1.0f;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public SlideView(Context context) {
        super(context);
        init();
    }

    public SlideView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlideView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private String searchQuery = "";
    
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        invalidate();
    }

    private GestureDetector gestureDetector;

    public interface OnContentLongClickListener {
        void onContentLongClick(String text);
    }
    
    private OnContentLongClickListener onContentLongClickListener;
    
    public void setOnContentLongClickListener(OnContentLongClickListener listener) {
        this.onContentLongClickListener = listener;
    }

    private int highlightColor = 0x80FFFF00; // Default generic
    private int activeHighlightColor = 0xFFFF0000; // Default generic target

    public void setHighlightColors(int highlightColor, int activeHighlightColor) {
        this.highlightColor = highlightColor;
        this.activeHighlightColor = activeHighlightColor;
        invalidate();
    }

    private void init() {
        // Default background
        setBackgroundColor(Color.WHITE);
        
        // Ensure we can receive long clicks
        setLongClickable(true);

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (slide == null) return;
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                // User requested "whole slide content copy feature" consistency.
                // Regardless of where they click, show the full text content in the dialog.
                if (slide.getContent() != null && !slide.getContent().isEmpty()) {
                    if (onContentLongClickListener != null) {
                        onContentLongClickListener.onContentLongClick(slide.getContent());
                    }
                } else {
                    Toast.makeText(getContext(), "No text content on this slide", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    // Allow GestureDetector to receive events
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true; 
    }

    public void setSlide(Slide slide) {
        this.slide = slide;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (slide == null || slide.getWidth() <= 0 || slide.getHeight() <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int width;
        
        if (widthMode == MeasureSpec.UNSPECIFIED || widthSize == 0) {
            // Fallback to Slide's intrinsic width converted to pixels
            float density = getResources().getDisplayMetrics().density;
            width = (int) (slide.getWidth() * density); 
        } else {
            width = widthSize;
        }

        // Calculate height based on slide aspect ratio
        float aspectRatio = slide.getHeight() / slide.getWidth();
        int height = (int) (width * aspectRatio);

        // Calculate scale factor
        scaleFactor = (float) width / slide.getWidth();

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slide == null || slide.getElements() == null) return;

        for (SlideElement element : slide.getElements()) {
            drawElement(canvas, element);
        }
        
        // Draw a border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(2);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    private void drawElement(Canvas canvas, SlideElement element) {
        RectF originalRect = element.getPosition();
        // Scale position to View coordinates
        RectF scaledRect = new RectF(
                originalRect.left * scaleFactor,
                originalRect.top * scaleFactor,
                originalRect.right * scaleFactor,
                originalRect.bottom * scaleFactor
        );

        if (element instanceof SlideElement.ShapeElement) {
            SlideElement.ShapeElement shape = (SlideElement.ShapeElement) element;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(shape.getColor());
            
            canvas.drawRect(scaledRect, paint);
            
        } else if (element instanceof SlideElement.ImageElement) {
            SlideElement.ImageElement image = (SlideElement.ImageElement) element;
            if (image.getBitmap() != null) {
                // Use a destination rect
                canvas.drawBitmap(image.getBitmap(), null, scaledRect, paint);
            }
        } else if (element instanceof SlideElement.TextElement) {
            SlideElement.TextElement text = (SlideElement.TextElement) element;
            textPaint.setColor(text.getColor());
            textPaint.setTextSize(text.getFontSize() * scaleFactor);
            textPaint.setTypeface(text.isBold() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

            int width = (int) scaledRect.width();
            if (width > 0) {
                // Create StaticLayout first to calculate highlights
                StaticLayout layout = new StaticLayout(
                        text.getText(), 
                        textPaint, 
                        width, 
                        Layout.Alignment.ALIGN_NORMAL, 
                        1.0f, 
                        0.0f, 
                        false);

                canvas.save();
                canvas.translate(scaledRect.left, scaledRect.top);

                // Highlight Search text precisely
                if (!searchQuery.isEmpty()) {
                    String fullText = text.getText().toLowerCase();
                    String query = searchQuery.toLowerCase();
                    int index = fullText.indexOf(query);
                    
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(highlightColor);
                    // Alpha is included in hex color (e.g. 0x4D......) or set externally
                    // paint.setAlpha(128); 

                    while (index >= 0) {
                        int end = index + query.length();
                        // Find lines for this match
                        int startLine = layout.getLineForOffset(index);
                        int endLine = layout.getLineForOffset(end);

                        for (int line = startLine; line <= endLine; line++) {
                            float lineTop = layout.getLineTop(line);
                            float lineBottom = layout.getLineBottom(line);
                            float startX = layout.getPrimaryHorizontal(Math.max(index, layout.getLineStart(line)));
                            float endX = layout.getPrimaryHorizontal(Math.min(end, layout.getLineEnd(line)));

                            canvas.drawRect(startX, lineTop, endX, lineBottom, paint);
                        }
                        
                        index = fullText.indexOf(query, index + 1);
                    }
                    paint.setAlpha(255);
                }

                layout.draw(canvas);
                canvas.restore();
            }
        }
    }
}
