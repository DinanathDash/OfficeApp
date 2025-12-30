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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecentFilesAdapter adapter;
    private TextView emptyView;
    private boolean isGridMode = false;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        // Load Recents
        loadRecentFiles();
        
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
        loadRecentFiles();

    }
    


    private void loadRecentFiles() {
        if (adapter == null) return;
        
        // Clean up any files that might have been deleted externally
        RecentFilesManager.getInstance(this).cleanUpInvalidFiles(this);
        
        List<RecentFile> files = RecentFilesManager.getInstance(this).getRecentFiles();
        adapter.updateList(files);
        if (emptyView != null) {
            emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void onFileSelected(Uri uri) {
        // Persist permission
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        String name = FileUtils.getFileName(this, uri);
        String ext = FileUtils.getFileExtension(this, uri);
        String type = (ext != null) ? ext.toUpperCase() : "FILE";

        // Add to recents
        // Open
        RecentFile recentFile = new RecentFile(name, uri.toString(), type);
        openFileViewer(recentFile);
    }

    private void openFileViewer(RecentFile file) {
        // Update Recents (move to top / update timestamp)
        // Re-create to update timestamp
        RecentFile updatedFile = new RecentFile(file.getName(), file.getUriString(), file.getType());
        RecentFilesManager.getInstance(this).addRecentFile(updatedFile);
        loadRecentFiles();

        // Toast.makeText(this, "Opening " + file.getName(), Toast.LENGTH_SHORT).show();
        
        Uri uri = Uri.parse(file.getUriString());
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
            startActivity(intent);
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


}