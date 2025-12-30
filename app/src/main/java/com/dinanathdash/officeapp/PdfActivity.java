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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.adapters.PdfPageAdapter;
import com.dinanathdash.officeapp.utils.FileUtils;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

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
    
    // Search related
    private Map<Integer, String> pageTextMap = new HashMap<>(); // Page Index -> Text
    private boolean isTextExtracted = false;
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;

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

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            try {
                openRenderer(uri);
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
        }
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
                return true; 
            }
        });

        searchView.setOnCloseListener(() -> {
            searchResults.clear();
            currentSearchIndex = -1;
            currentQuery = "";
            return false;
        });
        
        return true;
    }
    
    private void extractText(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                
                int pageCount = document.getNumberOfPages();
                android.util.Log.d("PdfActivity", "Starting extraction for " + pageCount + " pages");
                for (int i = 0; i < pageCount; i++) {
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String text = stripper.getText(document);
                    pageTextMap.put(i, text); // Store original text
                    if (i % 5 == 0) android.util.Log.d("PdfActivity", "Extracted page " + i);
                }
                document.close();
                isTextExtracted = true;
                android.util.Log.d("PdfActivity", "Extraction complete");
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    Toast.makeText(PdfActivity.this, "Search index ready", Toast.LENGTH_SHORT).show());
                
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("PdfActivity", "Extraction failed", e);
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
            Toast.makeText(this, "Found on page " + (searchResults.get(0) + 1) + " (" + searchResults.size() + " matches)", Toast.LENGTH_SHORT).show();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
