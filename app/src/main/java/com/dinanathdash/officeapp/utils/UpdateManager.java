package com.dinanathdash.officeapp.utils;

import android.content.Context;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.dinanathdash.officeapp.BuildConfig;
import com.dinanathdash.officeapp.R;
import com.dinanathdash.officeapp.ui.UpdateBottomSheetFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateManager {

    private static final String GITHUB_REPO = "DinanathDash/OfficeApp";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkForUpdates() {
        checkForUpdates(false);
    }

    public void checkForUpdates(boolean isSilent) {
        AlertDialog progressDialog = null;
        
        if (!isSilent) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_checking_updates, null);
            progressDialog = new MaterialAlertDialogBuilder(context)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
            progressDialog.show();
        }

        final AlertDialog finalProgressDialog = progressDialog;

        executor.execute(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String tagName = jsonObject.optString("tag_name", "");
                    String body = jsonObject.optString("body", "");
                    
                    // Find APK asset
                    String downloadUrl = "";
                    if (jsonObject.has("assets")) {
                        org.json.JSONArray assets = jsonObject.getJSONArray("assets");
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            if (asset.optString("name", "").toLowerCase().endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", "");
                                break;
                            }
                        }
                    }

                    // If no apk found, fallback to html_url (browser update)
                    if (downloadUrl.isEmpty()) {
                        downloadUrl = jsonObject.optString("html_url", "");
                    }
                    
                    String finalDownloadUrl = downloadUrl;
                    handler.post(() -> {
                        if (finalProgressDialog != null) {
                            finalProgressDialog.dismiss();
                        }
                        handleUpdateResponse(tagName, finalDownloadUrl, body, isSilent);
                    });
                } else {
                    handleError(finalProgressDialog, isSilent, "Code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                handleError(finalProgressDialog, isSilent, e.getLocalizedMessage());
            }
        });
    }

    private void handleError(AlertDialog dialog, boolean isSilent, String message) {
        handler.post(() -> {
            if (dialog != null) {
                dialog.dismiss();
            }
            if (!isSilent) {
                Toast.makeText(context, "Error checking for updates: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleUpdateResponse(String latestVersionTag, String downloadUrl, String notes, boolean isSilent) {
        String currentVersion = BuildConfig.VERSION_NAME;
        String cleanLatest = latestVersionTag.replace("v", "");
        String cleanCurrent = currentVersion.replace("v", "");

        if (!cleanLatest.equals(cleanCurrent)) {
             showUpdateDialog(latestVersionTag, downloadUrl, notes);
        } else {
            if (!isSilent) {
                Toast.makeText(context, "App is up to date (" + currentVersion + ")", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUpdateDialog(String version, String url, String notes) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            UpdateBottomSheetFragment.newInstance(version, url, notes)
                    .show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "UpdateDialog");
        } else {
            new MaterialAlertDialogBuilder(context)
                .setTitle("Update Available")
                .setMessage("A new version " + version + " is available.\n\n" + notes)
                .setPositiveButton("Update", (dialog, which) -> {
                    downloadAndInstall(context, url, version);
                })
                .setNegativeButton("Later", null)
                .show();
        }
    }
    
    public static void downloadAndInstall(Context context, String url, String version) {
        // If regular URL (not apk), open in browser
        if (!url.endsWith(".apk")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
            return;
        }

        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("App Update " + version);
        request.setDescription("Downloading " + version + "...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        String fileName = "officeapp_update_" + version + ".apk";
        
        // Use external public dir to ensure readability
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        // Register receiver for completion
        android.content.BroadcastReceiver onComplete = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context ctxt, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(ctxt, downloadId);
                    try {
                        ctxt.unregisterReceiver(this);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        };
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, new android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(onComplete, new android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private static void installApk(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = downloadManager.getUriForDownloadedFile(downloadId);

        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Installation failed. Please open Downloads folder and install manually.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
