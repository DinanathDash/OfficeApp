package com.dinanathdash.officeapp.data;

public class RecentFile {
    private String name;
    private String uriString;
    private long timestamp;
    private String type;

    public RecentFile(String name, String uriString, String type) {
        this.name = name;
        this.uriString = uriString;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
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
}
