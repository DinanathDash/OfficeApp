package com.dinanathdash.officeapp.data;

import android.graphics.Bitmap;
import android.graphics.RectF;

public abstract class SlideElement {
    protected RectF position;

    public SlideElement(RectF position) {
        this.position = position;
    }

    public RectF getPosition() {
        return position;
    }

    public static class TextElement extends SlideElement {
        private String text;
        private float fontSize;
        private int color;
        private boolean isBold;
        
        public TextElement(RectF position, String text, float fontSize, int color, boolean isBold) {
            super(position);
            this.text = text;
            this.fontSize = fontSize;
            this.color = color;
            this.isBold = isBold;
        }

        public String getText() { return text; }
        public float getFontSize() { return fontSize; }
        public int getColor() { return color; }
        public boolean isBold() { return isBold; }
    }

    public static class ImageElement extends SlideElement {
        private Bitmap bitmap;
        private byte[] imageData; // Keep raw data if needed for caching/lazy loading

        public ImageElement(RectF position, Bitmap bitmap) {
            super(position);
            this.bitmap = bitmap;
        }
        
        public Bitmap getBitmap() { return bitmap; }
    }
    
    public static class ShapeElement extends SlideElement {
        private int color;
        private int type; // 0=Rect, 1=Oval, etc.

        public ShapeElement(RectF position, int color, int type) {
            super(position);
            this.color = color;
            this.type = type;
        }
        
        public int getColor() { return color; }
        public int getType() { return type; }
    }
}
