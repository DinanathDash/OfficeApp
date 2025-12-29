package com.example.officeapp;

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

import com.example.officeapp.adapters.PdfPageAdapter;
import com.example.officeapp.utils.FileUtils;

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
    
    private void extractText(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                
                int pageCount = document.getNumberOfPages();
                for (int i = 0; i < pageCount; i++) {
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String text = stripper.getText(document);
                    pageTextMap.put(i, text.toLowerCase());
                }
                document.close();
                isTextExtracted = true;
                
            } catch (Exception e) {
                e.printStackTrace();
                // Fail silently, search will just not work
            }
        }).start();
    }

    private void openRenderer(Uri uri) throws IOException {
        fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        if (fileDescriptor != null) {
            pdfRenderer = new PdfRenderer(fileDescriptor);
            adapter = new PdfPageAdapter(pdfRenderer);
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

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        return true;
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
            if (entry.getValue().contains(lowerQuery)) {
                searchResults.add(entry.getKey());
            }
        }
        
        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
            currentSearchIndex = -1;
        } else {
            // Found matches
            currentSearchIndex = 0;
            int pageIndex = searchResults.get(0);
            scrollToPage(pageIndex);
            Toast.makeText(this, "Found on page " + (pageIndex + 1) + " (" + searchResults.size() + " matches)", Toast.LENGTH_SHORT).show();
        }
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
