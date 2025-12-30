package com.dinanathdash.officeapp.utils;

import android.content.Context;
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
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_checking_updates, null);

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        
        progressDialog.show();

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
                    String htmlUrl = jsonObject.optString("html_url", "");
                    String body = jsonObject.optString("body", "");

                    handler.post(() -> {
                        progressDialog.dismiss();
                        handleUpdateResponse(tagName, htmlUrl, body);
                    });
                } else if (responseCode == 404) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "No updates available", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to check for updates (Code: " + responseCode + ")", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Error checking for updates: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleUpdateResponse(String latestVersionTag, String downloadUrl, String notes) {
        String currentVersion = BuildConfig.VERSION_NAME;
        
        // Simple comparison: if strings are different, assume update (or strictly if not equal)
        // Ideally should parse semantic versioning, but inequality is safe enough for "New Version Available"
        // Removing 'v' prefix if present for comparison might be safer
        String cleanLatest = latestVersionTag.replace("v", "");
        String cleanCurrent = currentVersion.replace("v", "");

        if (!cleanLatest.equals(cleanCurrent)) {
             showUpdateDialog(latestVersionTag, downloadUrl, notes);
        } else {
            Toast.makeText(context, "App is up to date (" + currentVersion + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdateDialog(String version, String url, String notes) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            UpdateBottomSheetFragment.newInstance(version, url, notes)
                    .show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "UpdateDialog");
        } else {
            // Fallback for non-AppCompatActivity contexts (unlikely in this app but safe)
            new MaterialAlertDialogBuilder(context)
                .setTitle("Update Available")
                .setMessage("A new version " + version + " is available.\n\n" + notes)
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
        }
    }
}
