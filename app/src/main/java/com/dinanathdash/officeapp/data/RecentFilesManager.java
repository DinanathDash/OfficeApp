package com.dinanathdash.officeapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecentFilesManager {
    private static final String PREF_NAME = "office_app_prefs";
    private static final String KEY_RECENT_FILES = "recent_files";
    private static RecentFilesManager instance;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private RecentFilesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized RecentFilesManager getInstance(Context context) {
        if (instance == null) {
            instance = new RecentFilesManager(context);
        }
        return instance;
    }

    public synchronized List<RecentFile> getRecentFiles() {
        String json = sharedPreferences.getString(KEY_RECENT_FILES, null);
        Type type = new TypeToken<ArrayList<RecentFile>>() {}.getType();
        List<RecentFile> files = gson.fromJson(json, type);
        return files == null ? new ArrayList<>() : files;
    }

    public synchronized void removeRecentFile(RecentFile file) {
        List<RecentFile> files = getRecentFiles();
        files.removeIf(f -> f.getUriString().equals(file.getUriString()));
        saveRecentFiles(files);
    }

    public synchronized void addRecentFile(RecentFile file) {
        List<RecentFile> files = getRecentFiles();
        // Remove if exists to move to top
        files.removeIf(f -> f.getUriString().equals(file.getUriString()));
        files.add(0, file);
        
        if (files.size() > 20) { // Limit to 20
            files.remove(files.size() - 1);
        }
        
        saveRecentFiles(files);
    }
    
    private synchronized void saveRecentFiles(List<RecentFile> files) {
        String json = gson.toJson(files);
        sharedPreferences.edit().putString(KEY_RECENT_FILES, json).apply();
    }

    public synchronized void cleanUpInvalidFiles(Context context) {
        List<RecentFile> files = getRecentFiles();
        boolean changed = files.removeIf(file -> {
            try {
                return !com.dinanathdash.officeapp.utils.FileUtils.doesFileExist(context, android.net.Uri.parse(file.getUriString()));
            } catch (Exception e) {
                return true; // Remove on error
            }
        });

        if (changed) {
            saveRecentFiles(files);
        }
    }
}
