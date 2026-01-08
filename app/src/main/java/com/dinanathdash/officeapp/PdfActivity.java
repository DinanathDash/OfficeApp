package com.dinanathdash.officeapp;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.view.ViewGroup;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dinanathdash.officeapp.utils.ViewUtils;
import java.io.File;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.adapters.PdfPageAdapter;
import com.dinanathdash.officeapp.utils.FileUtils;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.dinanathdash.officeapp.utils.PdfHighlighter;
import android.graphics.RectF;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PdfPageAdapter adapter;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private com.airbnb.lottie.LottieAnimationView progressBar;
    
    // Search related
    private Map<Integer, String> pageTextMap = new HashMap<>(); // Page Index -> Text
    private boolean isTextExtracted = false;
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    
    // Holding document for coordinate extraction. 
    // WARNING: Memory intensive? PDDocument can be large.
    // Ideally we should reload page-by-page or cache coordinates.
    // For simplicity in this implementation, we keep it open or reload it.
    // Reloading per page render might be slow.
    // Let's keep a reference but ensure we close it.
    private PDDocument pdDocument;
    private PdfHighlighter highlighter;
    private Uri currentUri;
    
    private com.dinanathdash.officeapp.ui.GlobalZoomHelper globalZoomHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        
        PDFBoxResourceLoader.init(getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ViewUtils.applyBottomWindowInsets(recyclerView);
        
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            com.dinanathdash.officeapp.utils.LoaderUtils.applyThemeColors(progressBar, primaryColor);
        }

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            try {
                openRenderer(uri);
                openRenderer(uri);
                // openRenderer(uri) might fail if fileDescriptor is null, but we check.
                // We also need to open PDDocument for extraction.
                loadPdfDocument(uri);
                extractText(uri);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void openRenderer(Uri uri) throws IOException {
        fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        if (fileDescriptor != null) {
            pdfRenderer = new PdfRenderer(fileDescriptor);
            adapter = new PdfPageAdapter(pdfRenderer);
            adapter.setOnPageLongClickListener(this::showPageTextDialog);
            recyclerView.setAdapter(adapter);

            if (pdfRenderer.getPageCount() == 1) {
                ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                recyclerView.setLayoutParams(params);
            } else {
                 ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT; 
                recyclerView.setLayoutParams(params);
            }
            
            // Initialize Global Zoom
            globalZoomHelper = new com.dinanathdash.officeapp.ui.GlobalZoomHelper(this, recyclerView, adapter);
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (globalZoomHelper != null) {
            globalZoomHelper.processTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void showPageTextDialog(int pageIndex) {
        String text = pageTextMap.get(pageIndex);
        if (text == null || text.trim().isEmpty()) {
            if (!isTextExtracted) {
                Toast.makeText(this, "Text extracting, please wait...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No text found on this page.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        com.dinanathdash.officeapp.ui.TextBottomSheetFragment fragment = 
            com.dinanathdash.officeapp.ui.TextBottomSheetFragment.newInstance("Page " + (pageIndex + 1) + " Text", text);
        fragment.show(getSupportFragmentManager(), "ExtractedText");
    }

    private String currentQuery = "";

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                android.util.Log.d("PdfActivity", "Search submit: " + query + ", current: " + currentQuery);
                if (!query.equals(currentQuery)) {
                    currentQuery = query;
                    performSearch(query);
                } else {
                    navigateSearch(1);
                }
                return true; // We handled the submission
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Return true to indicate we handled it (by ignoring live search).
                // We do NOT update currentQuery here, otherwise submit will think we already searched it.
                // However, for highlighting to disappear when clearing:
                if (newText.isEmpty() && !currentQuery.isEmpty()) {
                   currentQuery = "";
                   adapter.setSearchQuery("", null);
                }
                return true; 
            }
        });

        searchView.setOnCloseListener(() -> {
            searchResults.clear();
            searchResults.clear();
            currentSearchIndex = -1;
            currentQuery = "";
            adapter.setSearchQuery("", null);
            return false;
        });
        
        return true;
    }
    
    private void loadPdfDocument(Uri uri) {
        currentUri = uri;
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> {
            try {
                 InputStream inputStream = getContentResolver().openInputStream(uri);
                 pdDocument = PDDocument.load(inputStream);
                 highlighter = new PdfHighlighter();
                 
                 // Pass document to adapter
                 runOnUiThread(() -> {
                     if (adapter != null) {
                         adapter.setPdfDocument(pdDocument, highlighter);
                         // Set colors
                         adapter.setHighlightColors(
                            androidx.core.content.ContextCompat.getColor(this, R.color.pdf_highlight),
                            androidx.core.content.ContextCompat.getColor(this, R.color.pdf_highlight_active)
                         );
                     }
                     if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                 });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                });
            }
        }).start();
    }

    private void extractText(Uri uri) {
        new Thread(() -> {
            try {
                // Use a separate document instance for bulk extraction to avoid threading issues if possible,
                // or just wait for loadPdfDocument.
                // Actually, PDDocument is not thread safe.
                // We should perform extraction on the same pdDocument instance if it's already loaded?
                // Or load a new one just for this one-off extraction. 
                // Loading a new one is safer for concurrency with the adapter/rendering thread if that ever accesses it.
                // But PDFBox memory usage is high.
                // Let's load a separate one for extraction and close it immediately.
                
                InputStream inputStream = getContentResolver().openInputStream(uri);
                PDDocument extractionDoc = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                
                int pageCount = extractionDoc.getNumberOfPages();
                android.util.Log.d("PdfActivity", "Starting extraction for " + pageCount + " pages");
                for (int i = 0; i < pageCount; i++) {
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String text = stripper.getText(extractionDoc);
                    pageTextMap.put(i, text); 
                }
                extractionDoc.close();
                isTextExtracted = true;
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void performSearch(String query) {
        if (!isTextExtracted) {
            Toast.makeText(this, "Preparing search index...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (query.trim().isEmpty()) return;
        
        searchResults.clear();
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<Integer, String> entry : pageTextMap.entrySet()) {
            if (entry.getValue().toLowerCase().contains(lowerQuery)) {
                searchResults.add(entry.getKey());
            }
        }
        
        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
            currentSearchIndex = -1;
        } else {
            // Found matches
            currentSearchIndex = 0;
            scrollToPage(searchResults.get(0));
            // Found matches
            currentSearchIndex = 0;
            scrollToPage(searchResults.get(0));
            Toast.makeText(this, "Found on page " + (searchResults.get(0) + 1) + " (" + searchResults.size() + " matches)", Toast.LENGTH_SHORT).show();
            
            // Pass query to adapter to start highlighting
            adapter.setSearchQuery(query, searchResults);
        }
    }

    private long lastNavTime = 0;

    private void navigateSearch(int direction) {
        if (searchResults.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavTime < 300) { // 300ms debounce
            return;
        }
        lastNavTime = currentTime;

        currentSearchIndex += direction;
        if (currentSearchIndex >= searchResults.size()) currentSearchIndex = 0;
        if (currentSearchIndex < 0) currentSearchIndex = searchResults.size() - 1;

        int pageIndex = searchResults.get(currentSearchIndex);
        scrollToPage(pageIndex);
        Toast.makeText(this, "Match " + (currentSearchIndex + 1) + " of " + searchResults.size() + " (Page " + (pageIndex + 1) + ")", Toast.LENGTH_SHORT).show();
    }
    
    private void scrollToPage(int pageIndex) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(pageIndex, 0);
        }
    }


    @Override
    protected void onDestroy() {
        if (adapter != null) {
            adapter.close();
        }
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
            if (pdDocument != null) {
                pdDocument.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
