package com.dinanathdash.officeapp.data;

public class RecentFile {
    private String name;
    private String uriString;
    private long timestamp;
    private String type;
    private String path;

    public RecentFile(String name, String uriString, String type, String path) {
        this.name = name;
        this.uriString = uriString;
        this.type = type;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Legacy constructor for backward compatibility if needed, using null path
    public RecentFile(String name, String uriString, String type) {
        this(name, uriString, type, null);
    }

    public String getName() {
        return name;
    }

    public String getUriString() {
        return uriString;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }
    
    public String getPath() {
        return path;
    }
}
