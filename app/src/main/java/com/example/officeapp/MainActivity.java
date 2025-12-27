package com.example.officeapp;

import android.content.Intent;
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

import com.example.officeapp.adapters.RecentFilesAdapter;
import com.example.officeapp.data.RecentFile;
import com.example.officeapp.data.RecentFilesManager;
import com.example.officeapp.utils.FileUtils;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecentFilesAdapter adapter;
    private TextView emptyView;

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

        // Setup Recycler View
        if (recyclerView != null) {
           recyclerView.setLayoutManager(new LinearLayoutManager(this));
           adapter = new RecentFilesAdapter(Collections.emptyList(), this::openFileViewer);
           recyclerView.setAdapter(adapter);
        }

        // Load Recents
        loadRecentFiles();

        // FAB Click
        if (fabOpen != null) {
            fabOpen.setOnClickListener(v -> {
                openFileLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword", "text/rtf", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "text/csv", 
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/vnd.ms-powerpoint",
                        "application/pdf", "*/*"});
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentFiles();
    }

    private void loadRecentFiles() {
        if (adapter == null) return;
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
        RecentFile recentFile = new RecentFile(name, uri.toString(), type);
        RecentFilesManager.getInstance(this).addRecentFile(recentFile);
        
        // Open
        openFileViewer(recentFile);
    }

    private void openFileViewer(RecentFile file) {
        // TODO: Implement Intent to open specific activities
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
}