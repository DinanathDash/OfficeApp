package com.example.officeapp.data;

public class Slide {
    private String title;
    private String content; // Bullet points or text
    private String notes;
    private int slideNumber;

    public Slide(int slideNumber, String title, String content, String notes) {
        this.slideNumber = slideNumber;
        this.title = title;
        this.content = content;
        this.notes = notes;
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
}
