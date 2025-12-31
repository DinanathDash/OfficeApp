package com.dinanathdash.officeapp.data;

public class Slide {
    private String title;
    private String content; // Bullet points or text
    private String notes;
    private int slideNumber;
    
    // New Rendering Data
    private java.util.List<SlideElement> elements;
    private float width; // Slide width in points/pixels
    private float height; // Slide height in points/pixels

    public Slide(int slideNumber, String title, String content, String notes) {
        // Legacy constructor adaptation
        this(slideNumber, title, content, notes, new java.util.ArrayList<>(), 0, 0);
    }
    
    public Slide(int slideNumber, String title, String content, String notes, 
                 java.util.List<SlideElement> elements, float width, float height) {
        this.slideNumber = slideNumber;
        this.title = title;
        this.content = content;
        this.notes = notes;
        this.elements = elements;
        this.width = width;
        this.height = height;
    }

    public int getSlideNumber() {
        return slideNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public String getNotes() {
        return notes;
    }
    
    public java.util.List<SlideElement> getElements() {
        return elements;
    }
    
    public float getWidth() { return width; }
    public float getHeight() { return height; }
}
