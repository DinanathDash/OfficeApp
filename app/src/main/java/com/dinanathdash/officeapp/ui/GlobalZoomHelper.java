package com.dinanathdash.officeapp.ui;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.OverScroller;
import androidx.core.view.ViewCompat;

/**
 * A helper to add global zoom support to a RecyclerView by scaling its content items.
 * Main advantage over wrapping RecyclerView in a ZoomLayout: Keeps Recycling mechanism alive.
 */
public class GlobalZoomHelper implements ScaleGestureDetector.OnScaleGestureListener {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;

    private final RecyclerView recyclerView;
    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private final ZoomableAdapter adapter;

    private float scaleFactor = 1.0f;
    private boolean isZooming = false;

    private final OverScroller scroller;
    
    // Scroll adjustment support
    private float focusYRatio = 0.5f; 

    private final Runnable flingRunnable = new Runnable() {
        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                int currX = scroller.getCurrX();
                // We are animating translationX. TranslationX is negative of scrollX conceptually.
                // But simplified: scroller goes from StartX to FinalX. 
                // We just setTranslationX(-currX).
                
                // Let's ensure constraints.
                int screenWidth = recyclerView.getContext().getResources().getDisplayMetrics().widthPixels;
                int contentWidth = recyclerView.getWidth();
                float minX = -(contentWidth - screenWidth);
                float maxX = 0;
                
                // We use scroller to track the positive scroll offset (0 to MaxScroll)
                // and map it to translationX (0 to -MaxScroll).
                
                float newTx = -currX;
                newTx = Math.min(newTx, maxX);
                newTx = Math.max(newTx, minX);
                
                recyclerView.setTranslationX(newTx);
                ViewCompat.postOnAnimation(recyclerView, this);
            }
        }
    }; 

    public interface ZoomableAdapter {
        void setGlobalScale(float scale);
    }

    public GlobalZoomHelper(Context context, RecyclerView recyclerView, ZoomableAdapter adapter) {
        this.recyclerView = recyclerView;
        this.adapter = adapter;
        this.scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (scaleFactor > 1.0f) {
                    float maxX = 0;
                    // contentWidth is always recyclerView.getWidth() which is expanded
                    int screenWidth = recyclerView.getContext().getResources().getDisplayMetrics().widthPixels;
                    int contentWidth = recyclerView.getWidth();
                    
                    if (contentWidth <= screenWidth) return false;
                    
                    float minX = -(contentWidth - screenWidth);
                    
                    // Add multiplier for sensitivity? 
                    // Usually 1:1 is best for direct touch, but if user says "needs more touch", 
                    // maybe we increase it slightly?
                    // float sensitivity = 1.0f;
                    
                    float newTx = recyclerView.getTranslationX() - distanceX;
                    newTx = Math.min(newTx, maxX);
                    newTx = Math.max(newTx, minX);
                    
                    recyclerView.setTranslationX(newTx);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                 if (scaleFactor > 1.0f) {
                     int screenWidth = recyclerView.getContext().getResources().getDisplayMetrics().widthPixels;
                     int contentWidth = recyclerView.getWidth();
                     if (contentWidth <= screenWidth) return false;
                     
                     // Current scroll position is -translationX.
                     // Min Scroll: 0
                     // Max Scroll: contentWidth - screenWidth
                     
                     int startX = (int) -recyclerView.getTranslationX();
                     int minX = 0;
                     int maxX = contentWidth - screenWidth;
                     
                     // Velocity needs to be negated?
                     // If we fling left (negative velocity), we want to scroll RIGHT (increase ScrollX).
                     // Scroller expects positive velocity to increase value.
                     
                     scroller.fling(startX, 0, (int) -velocityX, 0, minX, maxX, 0, 0);
                     ViewCompat.postOnAnimation(recyclerView, flingRunnable);
                     return true;
                 }
                 return false;
            }
        });
        
        this.scroller = new OverScroller(context);
    }



    public void processTouchEvent(MotionEvent event) {
         if (event.getAction() == MotionEvent.ACTION_DOWN) {
             if (!scroller.isFinished()) {
                 scroller.abortAnimation();
             }
         }
         scaleGestureDetector.onTouchEvent(event);
         gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float previousScale = scaleFactor;
        scaleFactor *= detector.getScaleFactor();
        scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

        if (previousScale != scaleFactor) {
            adapter.setGlobalScale(scaleFactor);
            
            // We adjust RecyclerView params width so it lays out content with more width
            int baseWidth = recyclerView.getContext().getResources().getDisplayMetrics().widthPixels;
            android.view.ViewGroup.LayoutParams rvParams = recyclerView.getLayoutParams();
            if (rvParams != null) {
                rvParams.width = (int) (baseWidth * scaleFactor);
                recyclerView.setLayoutParams(rvParams);
            }
            
            // Adjust translation to keep center or bounds?
            // When scaling down, if we were scrolled far right, we might need to clamp.
            int contentWidth = (int) (baseWidth * scaleFactor); // approximate current width after layout
            if (contentWidth > baseWidth) {
                 float minX = -(contentWidth - baseWidth);
                 float maxX = 0;
                 float tx = recyclerView.getTranslationX();
                 tx = Math.min(tx, maxX);
                 tx = Math.max(tx, minX);
                 recyclerView.setTranslationX(tx);
            } else {
                recyclerView.setTranslationX(0);
            }
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        isZooming = true;
        // Prevent HorizontalScrollView from intercepting while we are zooming
        if (recyclerView.getParent() != null) {
            recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isZooming = false;
    }
}
