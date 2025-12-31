package com.dinanathdash.officeapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import androidx.core.view.ViewCompat;

/**
 * A layout that supports pinch-to-zoom and panning for its first child view.
 * Behaves like a 2D ScrollView causing the child to be scrollable if larger than the parent.
 */
public class ZoomLayout extends FrameLayout implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "ZoomLayout";

    private enum Mode {
        NONE,
        DRAG,
        ZOOM,
        FLING
    }
    
    // Measurement configuration
    public enum MeasureMode {
        CONSTRAINED,       // Standard FrameLayout behavior (clamps to screen)
        UNBOUNDED_BOTH,    // Unspecified W & H (for huge tables/images)
        UNBOUNDED_VERTICAL // Constrained W, Unspecified H (for text wrapping + vertical scroll)
    }

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 8.0f;

    private Mode mode = Mode.NONE;
    private float scale = 1.0f;

    // Translation (Absolute translation of the child view)
    private float dx = 0f;
    private float dy = 0f;
    
    // Drag helpers
    private float prevDx = 0f;
    private float prevDy = 0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private OverScroller scroller;
    
    private MeasureMode measureMode = MeasureMode.CONSTRAINED;
    private boolean scrollableAtScaleOne = true; 

    public ZoomLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, this);
        scroller = new OverScroller(context);
        touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        
        // Ensure we can receive long clicks
        setLongClickable(true);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (mode == Mode.NONE) {
                    performLongClick(); // This calls the OnLongClickListener
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (scale > 1.0f) {
                    animateScaleAndTranslation(1.0f, 0f, 0f);
                } else {
                    float targetScale = 2.5f;
                    float focusX = e.getX();
                    float focusY = e.getY();
                    float targetDx = focusX - (focusX - dx) * (targetScale / scale);
                    float targetDy = focusY - (focusY - dy) * (targetScale / scale);
                    animateScaleAndTranslation(targetScale, targetDx, targetDy);
                }
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                 if (mode == Mode.DRAG) {
                     dx -= distanceX;
                     dy -= distanceY;
                     applyScaleAndTranslation();
                 }
                 return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (mode == Mode.DRAG || mode == Mode.NONE) {
                    
                    View child = child();
                    if (child == null) return false;
                    
                    float childWidth = child.getWidth() * scale;
                    float childHeight = child.getHeight() * scale;
                    float parentWidth = getWidth();
                    float parentHeight = getHeight();

                    // Calculate Bounds for Fling
                    // MinX = parentWidth - childWidth (negative), MaxX = 0
                    int minX = (int) (parentWidth - childWidth);
                    int maxX = 0;
                    int minY = (int) (parentHeight - childHeight);
                    int maxY = 0;
                    
                    if (minX > maxX) minX = maxX = 0; // Content fits X
                    if (minY > maxY) minY = maxY = 0; // Content fits Y
                    
                    scroller.fling((int) dx, (int) dy, (int) velocityX, (int) velocityY, 
                                   minX, maxX, minY, maxY);
                                   
                    mode = Mode.FLING;
                    ViewCompat.postInvalidateOnAnimation(ZoomLayout.this);
                    return true;
                }
                return false;
            }
        });
    }
    
    // Animation loop for Fling
    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            dx = scroller.getCurrX();
            dy = scroller.getCurrY();
            applyScaleAndTranslation();
            ViewCompat.postInvalidateOnAnimation(this);
        } else if (mode == Mode.FLING) {
            mode = Mode.NONE;
        }
    }
    
    public void setMeasureMode(MeasureMode mode) {
        this.measureMode = mode;
        requestLayout();
    }
    
    // Backwards compatibility
    public void setMeasureWithInfiniteBounds(boolean enable) {
        setMeasureMode(enable ? MeasureMode.UNBOUNDED_BOTH : MeasureMode.CONSTRAINED);
    }
    
    public void setScrollableAtScaleOne(boolean scrollable) {
        this.scrollableAtScaleOne = scrollable;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (measureMode == MeasureMode.CONSTRAINED) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        
        int widthMeasure = widthMeasureSpec; 
        int heightMeasure = heightMeasureSpec;

        if (measureMode == MeasureMode.UNBOUNDED_BOTH) {
             widthMeasure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
             heightMeasure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (measureMode == MeasureMode.UNBOUNDED_VERTICAL) {
             heightMeasure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasure, heightMeasure);
            }
        }
        
        // Parent always fills available space or wraps content if unbounded
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        if (measureMode == MeasureMode.UNBOUNDED_BOTH || measureMode == MeasureMode.UNBOUNDED_VERTICAL) {
            View child = child();
            if (child != null) {
                if (measureMode == MeasureMode.UNBOUNDED_BOTH) {
                    widthSize = resolveSize(child.getMeasuredWidth(), widthMeasureSpec);
                }
                heightSize = resolveSize(child.getMeasuredHeight(), heightMeasureSpec);
            }
        }
        
        setMeasuredDimension(widthSize, heightSize);
    }

    private View child() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (measureMode == MeasureMode.CONSTRAINED) {
                     child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
                } else {
                     child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
                }
                child.setPivotX(0);
                child.setPivotY(0);
            }
        }
    }

    private int touchSlop;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging;

    // ... (constructors call init)

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Updated: Removed the scale > 1.0f check to allow child interaction (selection) when zoomed.
        // if (scale > 1.0f) return true;
        
        if (ev.getPointerCount() > 1) return true;
        
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = ev.getX();
                lastTouchY = ev.getY();
                isDragging = false;
                
                // If we are currently flinging, we need to intercept immediately so we can
                // stop the fling and potentially start a new drag.
                if (!scroller.isFinished()) {
                    isDragging = true;
                    return true;
                }
                
                // Don't intercept DOWN, let child have it. 
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging) return true; // Already dragging
                
                final float x = ev.getX();
                final float y = ev.getY();
                final float xDiff = Math.abs(x - lastTouchX);
                final float yDiff = Math.abs(y - lastTouchY);
                
                if (xDiff > touchSlop || yDiff > touchSlop) {
                    isDragging = true;
                    
                    // Synthesize a DOWN event for the detectors because they missed the actual DOWN
                    // (it was consumed by the child). This ensures smooth panning initialization.
                    MotionEvent downEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), 
                            MotionEvent.ACTION_DOWN, lastTouchX, lastTouchY, 0);
                    scaleDetector.onTouchEvent(downEvent);
                    gestureDetector.onTouchEvent(downEvent);
                    downEvent.recycle();

                    return true; // Start intercepting
                }
                break;
                
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
        }
        
        return isDragging;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        
        switch(event.getAction() & MotionEvent.ACTION_MASK) {
             case MotionEvent.ACTION_DOWN:
                 if (!scroller.isFinished()) {
                     scroller.abortAnimation();
                 }
                 mode = Mode.DRAG;
                 prevDx = dx;
                 prevDy = dy;
                 break;
                 
             case MotionEvent.ACTION_POINTER_DOWN:
                 mode = Mode.ZOOM;
                 break;
                 
             case MotionEvent.ACTION_POINTER_UP:
                 mode = Mode.DRAG; 
                 // Reset prevDx to avoid jumps?
                 // Usually calculating delta in onScroll handles this.
                 break;
                 
             case MotionEvent.ACTION_UP:
                 if (mode != Mode.FLING) {
                     mode = Mode.NONE;
                 }
                 break;
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float previousScale = scale;
        
        scale *= scaleFactor;
        scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
        
        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();
        
        dx = focusX - (focusX - dx) * (scale / previousScale);
        dy = focusY - (focusY - dy) * (scale / previousScale);
        
        applyScaleAndTranslation();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mode = Mode.ZOOM;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    private void applyScaleAndTranslation() {
        View child = child();
        if (child == null) return;

        child.setScaleX(scale);
        child.setScaleY(scale);
        
        float childWidth = child.getWidth() * scale;
        float childHeight = child.getHeight() * scale;
        float parentWidth = getWidth();
        float parentHeight = getHeight();
        
        // Clamp Translation
        
        if (childWidth <= parentWidth) {
            dx = 0; 
        } else {
            float minDx = parentWidth - childWidth;
            float maxDx = 0;
            dx = Math.min(dx, maxDx);
            dx = Math.max(dx, minDx);
        }
        
        if (childHeight <= parentHeight) {
            dy = 0;
        } else {
            float minDy = parentHeight - childHeight;
            float maxDy = 0;
            dy = Math.min(dy, maxDy);
            dy = Math.max(dy, minDy);
        }

        child.setTranslationX(dx);
        child.setTranslationY(dy);
    }
    
    public void scrollToPosition(float x, float y, boolean animate) {
        float targetDx = -x * scale;
        float targetDy = -y * scale;
        
        if (animate) {
            // Simple animation or just reuse the scale animation helper with current scale
            animateScaleAndTranslation(this.scale, targetDx, targetDy);
        } else {
            this.dx = targetDx;
            this.dy = targetDy;
            applyScaleAndTranslation();
        }
    }

    private void animateScaleAndTranslation(float targetScale, float targetDx, float targetDy) {
        this.scale = targetScale;
        this.dx = targetDx;
        this.dy = targetDy;
        applyScaleAndTranslation();
    }
}
