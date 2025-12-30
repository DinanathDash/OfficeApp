package com.dinanathdash.officeapp.data;

public class Folder {
    private String name;
    private String path;
    private int fileCount;
    private String size;

    public Folder(String name, String path, int fileCount, String size) {
        this.name = name;
        this.path = path;
        this.fileCount = fileCount;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getFileCount() {
        return fileCount;
    }

    public String getSize() {
        return size;
    }
}
