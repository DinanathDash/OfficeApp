package com.dinanathdash.officeapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.adapters.RecentFilesAdapter;
import com.dinanathdash.officeapp.data.RecentFile;
import com.dinanathdash.officeapp.data.RecentFilesManager;
import com.dinanathdash.officeapp.utils.FileUtils;
import com.dinanathdash.officeapp.utils.UpdateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Environment;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private RecentFilesAdapter adapter;
    private TextView emptyView;
    private boolean isGridMode = false;
    private boolean isPermissionDialogShowing = false;

    // File Picker Result
    private final ActivityResultLauncher<String[]> openFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    onFileSelected(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Window Insets
        // Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // No bottom padding regarding system bars for root
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nestedScrollView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomPadding = (int) (24 * v.getResources().getDisplayMetrics().density);
            v.setPadding(0, 0, 0, systemBars.bottom + bottomPadding); 
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fabOpen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            // 24dp (original margin) + bottom inset
            int originalMargin = (int) (24 * getResources().getDisplayMetrics().density);
            params.bottomMargin = originalMargin + systemBars.bottom;
            v.setLayoutParams(params);
            return insets;
        });

        // Check for updates silently in the background
        UpdateManager updateManager = new UpdateManager(this);
        updateManager.checkForUpdates(true);

        // Initialize UI
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        View fabOpen = findViewById(R.id.fabOpen);
        
        
        // Search Setup
        android.widget.EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFiles(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
        
        // Setup Recycler View (Recent)
        if (recyclerView != null) {
           recyclerView.setLayoutManager(new LinearLayoutManager(this));
           adapter = new RecentFilesAdapter(new ArrayList<>(), new RecentFilesAdapter.OnFileActionListener() {
               @Override
               public void onItemClick(com.dinanathdash.officeapp.data.RecentFile file) {
                   openFileViewer(file);
               }

               @Override
               public void onShareFile(com.dinanathdash.officeapp.data.RecentFile file) {
                   shareFile(file);
               }

               @Override
                public void onDeleteFile(com.dinanathdash.officeapp.data.RecentFile file) {
                    // Start by removing from recent to ensure UI updates immediately
                    RecentFilesManager.getInstance(MainActivity.this).removeRecentFile(file);
                    loadRecentFiles(); 
                    
                    // Attempt to delete actual file
                    boolean deleted = false;
                    try {
                        Uri uri = Uri.parse(file.getUriString());
                        deleted = FileUtils.deleteFile(MainActivity.this, uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    String msg = deleted ? "File deleted" : "File removed from list";
                    android.widget.Toast.makeText(MainActivity.this, msg, android.widget.Toast.LENGTH_SHORT).show();
                }
               @Override
               public void onDetailsFile(com.dinanathdash.officeapp.data.RecentFile file) {
                   com.dinanathdash.officeapp.ui.FileDetailsBottomSheetFragment fragment = 
                       com.dinanathdash.officeapp.ui.FileDetailsBottomSheetFragment.newInstance(file);
                   fragment.show(getSupportFragmentManager(), "FileDetails");
               }
           });
           recyclerView.setAdapter(adapter);
        }
        
        // Header More Options
        View ivRecentFilesMore = findViewById(R.id.ivRecentFilesMore);
        if (ivRecentFilesMore != null) {
            ivRecentFilesMore.setOnClickListener(v -> showHeaderPopup(v));
        }
        
        // Profile / About Screen
        View ivProfile = findViewById(R.id.ivProfile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        }

        // Load Recents with strict cleanup on startup
        performStartupCleanup();
        
        // FAB Click
        if (fabOpen != null) {
            fabOpen.setOnClickListener(v -> {
                openFileLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                        "application/msword", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                        "application/vnd.ms-excel", 
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation", 
                        "application/vnd.ms-powerpoint", 
                        "text/plain", "text/csv", "text/rtf",
                        "application/pdf", "*/*"});
            });
        }
        
        // Handle incoming intent (Open from other apps)
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();
            
            if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) && intent.getData() != null) {
                onFileSelected(intent.getData());
            }
        }
    }
    


    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        loadRecentFiles();

    }
    


    private void loadRecentFiles() {
        if (adapter == null) return;
        
        // Load immediately from cache for speed
        List<RecentFile> cachedFiles = RecentFilesManager.getInstance(this).getRecentFiles();
        adapter.updateList(cachedFiles);
        if (emptyView != null) {
            emptyView.setVisibility(cachedFiles.isEmpty() ? View.VISIBLE : View.GONE);
        }

        // Clean up
        // If this is the FIRST load (e.g. Activity creation), we want to enforce local only
        // effectively clearing out session-temporary files.
        // We can track if it's "start up" via a flag or simply call a specific cleanup method in onCreate.
        // But since loadRecentFiles is called often, let's just do "standard" cleanup (validation) here.
        // We will call the STRICT cleanup from onCreate specially.
        
        new Thread(() -> {
            RecentFilesManager.getInstance(this).cleanUpInvalidFiles(this, false); // False = just validate existence
            
            // Post update to UI
            runOnUiThread(() -> {
                List<RecentFile> files = RecentFilesManager.getInstance(this).getRecentFiles();
                if (adapter != null) {
                    adapter.updateList(files);
                }
                if (emptyView != null) {
                    emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }
    
    private void performStartupCleanup() {
         new Thread(() -> {
            // True = remove anything not local (Clean session files)
            RecentFilesManager.getInstance(this).cleanUpInvalidFiles(this, true);
            
            runOnUiThread(() -> {
                 loadRecentFiles();
            });
         }).start();
    }

    private void onFileSelected(Uri uri) {
        // Persist permission
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Expected for some 3rd party providers like WhatsApp
        }

        String name = FileUtils.getFileName(this, uri);
        String ext = FileUtils.getFileExtension(this, uri);
        String type = (ext != null) ? ext.toUpperCase() : "FILE";
        String path = FileUtils.getPath(this, uri);

        // Smart Match Logic (Case 2 fix): Check if this is a temp URI but file is actually downloaded
        Uri finalUri = uri;
        String finalPath = path;
        
        // If the URI is NOT local (e.g., Gmail temp provider), try to find a local match
        if (!FileUtils.isLocalFile(this, uri)) {
            long fileSize = FileUtils.getFileSize(this, uri);
            Uri localMatch = FileUtils.findLocalFileMatch(this, name, fileSize);
            
            if (localMatch != null) {
                // Found a local file that matches! Use it instead
                finalUri = localMatch;
                finalPath = localMatch.getPath();
            }
        }

        // Add to recents
        // Open
        RecentFile recentFile = new RecentFile(name, finalUri.toString(), type, finalPath);
        openFileViewer(recentFile);
    }

    private void openFileViewer(RecentFile file) {
        // Fast check before opening (with path fallback for Case 3)
        Uri uri = Uri.parse(file.getUriString());
        FileUtils.FileStatus status = FileUtils.checkFileStatusWithPath(this, uri, file.getPath());
        
        if (status == FileUtils.FileStatus.NOT_FOUND) {
            Toast.makeText(this, "File not found during open check", Toast.LENGTH_SHORT).show();
            RecentFilesManager.getInstance(this).removeRecentFile(file);
            loadRecentFiles();
            return;
        }

        // Update Recents (move to top / update timestamp)
        // Re-create to update timestamp
        RecentFile updatedFile = new RecentFile(file.getName(), file.getUriString(), file.getType(), file.getPath());
        RecentFilesManager.getInstance(this).addRecentFile(updatedFile);
        loadRecentFiles();

        // Toast.makeText(this, "Opening " + file.getName(), Toast.LENGTH_SHORT).show();
        

        String type = file.getType();
        
        Intent intent = null;
        if (type.contains("DOC") || type.contains("RTF")) {
             intent = new Intent(this, WordActivity.class);
        } else if (type.contains("XLS") || type.contains("CSV") || type.contains("SHEET")) {
             intent = new Intent(this, ExcelActivity.class);
        } else if (type.contains("PPT") || type.contains("PRESENTATION") || type.contains("POT")) {
             intent = new Intent(this, PptActivity.class);
        } else if (type.contains("PDF")) {
             intent = new Intent(this, PdfActivity.class);
        } else if (type.contains("TXT") || type.contains("TEXT")) {
             intent = new Intent(this, TxtActivity.class);
        } else {
             Toast.makeText(this, "Unsupported file type: " + type, Toast.LENGTH_SHORT).show();
        }
        
        if (intent != null) {
            intent.setData(uri);
            // We need to grant read permission to the target activity
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                // Try Fallback with Path if available
                boolean fallbackSuccess = false;
                if (file.getPath() != null) {
                    try {
                        java.io.File f = new java.io.File(file.getPath());
                        if (f.exists()) {
                            Uri fileUri = Uri.fromFile(f);
                            intent.setData(fileUri);
                            // File URI doesn't need grant flags usually if we have permission, but keep simple
                            startActivity(intent);
                            fallbackSuccess = true;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (!fallbackSuccess) {
                    Toast.makeText(this, "Permission lost. Please re-open file from Files app.", Toast.LENGTH_LONG).show();
                    // Do NOT remove. User knows it exists.
                }
            } catch (Exception e) {
                Toast.makeText(this, "Could not open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private String currentTypeFilter = null;

    private void filterFiles(String query) {
        applyFilters(query, currentTypeFilter);
    }
    
    private void applyFilters(String searchQuery, String typeFilter) {
        List<RecentFile> allFiles = RecentFilesManager.getInstance(this).getRecentFiles();
        List<RecentFile> filtered = new ArrayList<>();
        
        for (RecentFile file : allFiles) {
            boolean matchesSearch = (searchQuery == null || searchQuery.isEmpty()) || 
                                    file.getName().toLowerCase().contains(searchQuery.toLowerCase());
                                    
            boolean matchesType = true;
            if (typeFilter != null) {
                String ext = FileUtils.getFileExtension(this, Uri.parse(file.getUriString()));
                if (ext == null) ext = "";
                ext = ext.toUpperCase();
                
                switch (typeFilter) {
                    case "DOCUMENT":
                        matchesType = ext.equals("DOC") || ext.equals("DOCX") || ext.equals("RTF");
                        break;
                    case "PDF":
                        matchesType = ext.equals("PDF");
                        break;
                    case "SPREADSHEET":
                        matchesType = ext.equals("XLS") || ext.equals("XLSX") || ext.equals("CSV") || ext.contains("SHEET");
                        break;
                    case "PRESENTATION":
                        matchesType = ext.equals("PPT") || ext.equals("PPTX") || ext.equals("POT") || ext.contains("PRES");
                        break;
                    case "TXT":
                        matchesType = ext.equals("TXT");
                        break;
                    case "OTHER":
                         matchesType = ! (ext.equals("DOC") || ext.equals("DOCX") || ext.equals("RTF") ||
                                          ext.equals("PDF") ||
                                          ext.equals("XLS") || ext.equals("XLSX") || ext.equals("CSV") || ext.contains("SHEET") ||
                                          ext.equals("PPT") || ext.equals("PPTX") || ext.equals("POT") || ext.contains("PRES") ||
                                          ext.equals("TXT"));
                        break;
                }
            }
            
            if (matchesSearch && matchesType) {
                filtered.add(file);
            }
        }
        adapter.filterList(filtered);
    }

    private void showHeaderPopup(View v) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_recent_header, null);
        
        // Measure content to calculate width and offset correctly considering the new CardView margins
        // Calculate width in pixels (300dp)
        float density = getResources().getDisplayMetrics().density;
        int width = (int) (180 * density);

        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                popupView, 
                width, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);
                
        popupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.popup_bg));
        popupWindow.setElevation(16);

        // View Mode Toggles
        // View Mode Toggles
        android.widget.ImageView btnList = popupView.findViewById(R.id.action_view_list);
        android.widget.ImageView btnGrid = popupView.findViewById(R.id.action_view_grid);
        
        if (isGridMode) {
             btnList.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
             btnGrid.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
        } else {
             btnList.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
             btnGrid.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        }

        btnList.setOnClickListener(view -> {
             if (isGridMode) toggleViewMode(false);
             popupWindow.dismiss();
        });
        btnGrid.setOnClickListener(view -> {
             if (!isGridMode) toggleViewMode(true);
             popupWindow.dismiss();
        });

        // Helper to set selection state
        View.OnClickListener filterListener = view -> {
            int id = view.getId();
            if (id == R.id.filter_doc) currentTypeFilter = "DOCUMENT";
            else if (id == R.id.filter_pdf) currentTypeFilter = "PDF";
            else if (id == R.id.filter_sheet) currentTypeFilter = "SPREADSHEET";
            else if (id == R.id.filter_presentation) currentTypeFilter = "PRESENTATION";
            else if (id == R.id.filter_txt) currentTypeFilter = "TXT";
            else if (id == R.id.filter_other) currentTypeFilter = "OTHER";
            else if (id == R.id.filter_clear) currentTypeFilter = null;
            
            // Re-apply filter
            android.widget.EditText etSearch = findViewById(R.id.etSearch);
            applyFilters(etSearch.getText() != null ? etSearch.getText().toString() : "", currentTypeFilter);
            
            popupWindow.dismiss();
        };

        // Initialize items with selection state
        setupFilterItem(popupView, R.id.filter_doc, "DOCUMENT", filterListener);
        setupFilterItem(popupView, R.id.filter_pdf, "PDF", filterListener);
        setupFilterItem(popupView, R.id.filter_sheet, "SPREADSHEET", filterListener);
        setupFilterItem(popupView, R.id.filter_presentation, "PRESENTATION", filterListener);
        setupFilterItem(popupView, R.id.filter_txt, "TXT", filterListener);
        setupFilterItem(popupView, R.id.filter_other, "OTHER", filterListener);
        
        // Clear filter setup
        popupView.findViewById(R.id.filter_clear).setOnClickListener(filterListener);

        // Positioning Logic
        // Calculate X offset to align the END of the popup with the END of the anchor view
        // margin accounts for a small gap from the edge if needed
        int xOff = 0;
        int anchorWidth = v.getWidth();
        // We want (anchorX + anchorWidth) - (popupX + popupWidth) = 0 (aligned right edges)
        // showAsDropDown places popup at anchorX + xOff
        // So: anchorX + anchorWidth = (anchorX + xOff) + popupWidth
        // xOff = anchorWidth - popupWidth
        
        xOff = anchorWidth - width;
        
        // You might want a slight offset from the very edge
        // xOff -= 16; 

        popupWindow.showAsDropDown(v, xOff, 0); 
    }

    private void toggleViewMode(boolean enableGrid) {
        isGridMode = enableGrid;
        
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            if (isGridMode) {
                recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            
            if (adapter != null) {
                adapter.setGridMode(isGridMode);
            }
        }
    }
    
    private void setupFilterItem(View parent, int viewId, String filterType, View.OnClickListener listener) {
        TextView tv = parent.findViewById(viewId);
        tv.setOnClickListener(listener);
        
        boolean isSelected = filterType.equals(currentTypeFilter);
        
        if (isSelected) {
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
             tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
             tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
    
    private void shareFile(RecentFile file) {
        try {
            Uri fileUri = Uri.parse(file.getUriString());
            Uri contentUri;
            
            // If it's a file:// URI, we must use FileProvider
            if ("file".equals(fileUri.getScheme())) {
                java.io.File f = new java.io.File(fileUri.getPath());
                contentUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        f);
            } else {
                contentUri = fileUri;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            String mimeType = FileUtils.getMimeType(this, contentUri);
            if (mimeType == null) {
                mimeType = "*/*";
            }
            
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            
            // Add ClipData with the correct display name
            String displayName = file.getName();
            // If the name doesn't have an extension but we know the type, we might want to append it, 
            // but usually the name from RecentFile already has it (or is the display name).
            
            android.content.ClipData clipData = new android.content.ClipData(
                    new android.content.ClipDescription(displayName, new String[]{mimeType}),
                    new android.content.ClipData.Item(contentUri)
            );
            shareIntent.setClipData(clipData);
            
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share file using"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }




    private AlertDialog onboardingDialog;

    private void checkPermissions() {
        boolean isStorageGranted = Environment.isExternalStorageManager();
        boolean isInstallGranted = getPackageManager().canRequestPackageInstalls();

        if (isStorageGranted && isInstallGranted) {
            if (onboardingDialog != null && onboardingDialog.isShowing()) {
                onboardingDialog.dismiss();
            }
            return;
        }

        showOnboardingDialog(isStorageGranted, isInstallGranted);
    }

    private void showOnboardingDialog(boolean isStorageGranted, boolean isInstallGranted) {
        if (onboardingDialog != null && onboardingDialog.isShowing()) {
            updateOnboardingDialogUI(isStorageGranted, isInstallGranted);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_permission_onboarding, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // Mandatory

        onboardingDialog = builder.create();
        // Transparent background for the card view to look right
        if (onboardingDialog.getWindow() != null) {
            onboardingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Initialize UI
        updateOnboardingUI(dialogView, isStorageGranted, isInstallGranted);

        onboardingDialog.show();
    }

    private void updateOnboardingDialogUI(boolean isStorageGranted, boolean isInstallGranted) {
        if (onboardingDialog == null) return;
        // In case the dialog view needs to be refreshed
        // Since we are not re-inflating, we just update the existing view
        // But we need reference to the view. 
        // AlertDialog doesn't expose getView() easily unless we keep it or find by id window
        // Better to just update if we can access the view elements, but simpler to just re-bind if we kept reference
        // OR better: Just findViewById on the dialog itself
        
        updateOnboardingUI(onboardingDialog.findViewById(android.R.id.content), isStorageGranted, isInstallGranted);
    }
    
    private void updateOnboardingUI(View root, boolean isStorageGranted, boolean isInstallGranted) {
        if (root == null) return;

        View btnGrantStorage = root.findViewById(R.id.btnGrantStorage);
        View imgCheckStorage = root.findViewById(R.id.imgCheckStorage);
        View btnGrantInstall = root.findViewById(R.id.btnGrantInstall);
        View imgCheckInstall = root.findViewById(R.id.imgCheckInstall);

        // Storage State
        if (isStorageGranted) {
            btnGrantStorage.setVisibility(View.GONE);
            imgCheckStorage.setVisibility(View.VISIBLE);
        } else {
            btnGrantStorage.setVisibility(View.VISIBLE);
            imgCheckStorage.setVisibility(View.GONE);
            btnGrantStorage.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            });
        }

        // Install State
        if (isInstallGranted) {
            btnGrantInstall.setVisibility(View.GONE);
            imgCheckInstall.setVisibility(View.VISIBLE);
        } else {
            btnGrantInstall.setVisibility(View.VISIBLE);
            imgCheckInstall.setVisibility(View.GONE);
            // Optional: Disable install grant until storage is granted? 
            // User requested "one by one", but didn't strictly say sequential enforcement.
            // But let's keep them independent as requested "click on access to one by one".
            
            btnGrantInstall.setOnClickListener(v -> {
                 try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                 } catch (Exception e) {
                     e.printStackTrace();
                     Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show();
                 }
            });
        }
    }
}