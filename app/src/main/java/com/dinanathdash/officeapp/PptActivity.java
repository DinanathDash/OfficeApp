package com.dinanathdash.officeapp;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.adapters.SlidesAdapter;
import com.dinanathdash.officeapp.data.Slide;
import com.dinanathdash.officeapp.utils.FileUtils;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PptActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SlidesAdapter adapter;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<Slide> allSlides = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlidesAdapter(Collections.emptyList());
        recyclerView.setAdapter(adapter);

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            loadPresentation(uri);
        } else {
            finish();
        }
    }

    private void loadPresentation(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            List<Slide> slides = new ArrayList<>();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Cannot open file");
                
                String extension = FileUtils.getFileExtension(this, uri);
                
                if (extension != null && extension.equalsIgnoreCase("pptx")) {
                    parsePptx(inputStream, slides);
                } else if (extension != null && (extension.equalsIgnoreCase("ppt") || extension.equalsIgnoreCase("pot"))) {
                    parsePpt(inputStream, slides);
                }
                
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(PptActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            allSlides = new ArrayList<>(slides); // Store copy of all slides

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(slides);
                progressBar.setVisibility(View.GONE);
                updateSubtitle(slides.size());
                
                if (slides.size() == 1) {
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    recyclerView.setLayoutParams(params);
                } else {
                     ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    recyclerView.setLayoutParams(params);
                }
            });
        });
    }

    private void updateSubtitle(int count) {
        if(getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(count + " Slides");
        }
    }

    private void parsePptx(InputStream inputStream, List<Slide> slidesCollector) throws Exception {
        XMLSlideShow ppt = new XMLSlideShow(inputStream);
        List<XSLFSlide> slides = ppt.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            String title = slide.getTitle();
            StringBuilder content = new StringBuilder();
            
            // Basic text extraction from shapes
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    // Avoid duplicating title in content if possible, but simplest is just dump all text
                    if (!textShape.getText().equals(title)) {
                        content.append("• ").append(textShape.getText()).append("\n");
                    }
                }
            }
            
            // Notes could be extracted from slide.getNotes() but simplified here.
            
            slidesCollector.add(new Slide(i + 1, title, content.toString().trim(), ""));
        }
    }



    private void parsePpt(InputStream inputStream, List<Slide> slidesCollector) throws Exception {
        HSLFSlideShow ppt = new HSLFSlideShow(inputStream);
        List<HSLFSlide> slides = ppt.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            HSLFSlide slide = slides.get(i);
            String title = slide.getTitle();
            StringBuilder content = new StringBuilder();
            
            for (HSLFShape shape : slide.getShapes()) {
                 if (shape instanceof HSLFTextShape) {
                     HSLFTextShape textShape = (HSLFTextShape) shape;
                     if(title == null || !textShape.getText().equals(title)) {
                         content.append("• ").append(textShape.getText()).append("\n");
                     }
                 }
            }
            
            String notes = "";
            if (slide.getNotes() != null) {
                for (HSLFShape shape : slide.getNotes().getShapes()) {
                     if (shape instanceof HSLFTextShape) {
                         notes += ((HSLFTextShape) shape).getText() + "\n";
                     }
                }
            }
            
            slidesCollector.add(new Slide(i + 1, title, content.toString().trim(), notes.trim()));
        }
    }

    private String currentQuery = "";
    private static class PptMatch {
        int slideIndex;
        int fieldType; // 0=Title, 1=Content, 2=Notes
        int startIndex;
        
        public PptMatch(int slideIndex, int fieldType, int startIndex) {
            this.slideIndex = slideIndex;
            this.fieldType = fieldType;
            this.startIndex = startIndex;
        }
    }

    private List<PptMatch> allMatches = new ArrayList<>();
    // private List<Integer> matchSlides = new ArrayList<>(); // Removed in favor of allMatches
    private int currentMatchIndex = -1;

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.equals(currentQuery)) {
                    currentQuery = query;
                    findMatches(query);
                } else {
                    navigateSearch(1);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Live highlighting, but don't jump yet
                // We do NOT update currentQuery here, so Submit will trigger findMatches (which populates matchSlides)
                adapter.setSearchQuery(newText);
                adapter.notifyDataSetChanged();
                return true;
            }
        });
        
        searchView.setOnCloseListener(() -> {
            currentQuery = "";
            adapter.setSearchQuery("");
            adapter.updateList(allSlides); // Ensure full list is shown
            allMatches.clear();
            return false;
        });
        
        return true;
    }
    
    private void findMatches(String query) {
        if (allSlides == null || allSlides.isEmpty()) return;
        
        // Ensure we are showing all slides, not filtered
        adapter.updateList(allSlides);
        adapter.setSearchQuery(query);
        adapter.notifyDataSetChanged();
        
        allMatches.clear();
        String lowerCaseQuery = query.toLowerCase();

        for (int i = 0; i < allSlides.size(); i++) {
            Slide slide = allSlides.get(i);
            
            // Title (0)
            if (slide.getTitle() != null) {
                findAllIndices(allMatches, i, 0, slide.getTitle().toLowerCase(), lowerCaseQuery);
            }
            
            // Content (1)
            if (slide.getContent() != null) {
                findAllIndices(allMatches, i, 1, slide.getContent().toLowerCase(), lowerCaseQuery);
            }
            
            // Notes (2)
            if (slide.getNotes() != null) {
                findAllIndices(allMatches, i, 2, slide.getNotes().toLowerCase(), lowerCaseQuery);
            }
        }
        
        if (!allMatches.isEmpty()) {
            currentMatchIndex = 0;
            navigateToMatch(currentMatchIndex);
            Toast.makeText(this, "Found " + allMatches.size() + " matches", Toast.LENGTH_SHORT).show();
        } else {
             // Reset active
             adapter.setActiveMatch(-1, -1, -1);
             Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
        }
        updateSubtitle(allSlides.size());
    }
    
    private void findAllIndices(List<PptMatch> matches, int slideIndex, int fieldType, String text, String query) {
        int index = text.indexOf(query);
        while (index >= 0) {
            matches.add(new PptMatch(slideIndex, fieldType, index));
            index = text.indexOf(query, index + query.length());
        }
    }
    
    private long lastNavTime = 0;
    
    private void navigateSearch(int direction) {
        if (allMatches.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavTime < 300) { // 300ms debounce
            return;
        }
        lastNavTime = currentTime;
        
        currentMatchIndex += direction;
        if (currentMatchIndex >= allMatches.size()) currentMatchIndex = 0;
        if (currentMatchIndex < 0) currentMatchIndex = allMatches.size() - 1;
        
        navigateToMatch(currentMatchIndex);
        Toast.makeText(this, "Match " + (currentMatchIndex + 1) + " of " + allMatches.size(), Toast.LENGTH_SHORT).show();
    }
    
    private void navigateToMatch(int index) {
        PptMatch match = allMatches.get(index);
        scrollToSlide(match.slideIndex);
        adapter.setActiveMatch(match.slideIndex, match.fieldType, match.startIndex);
    }
    
    private void scrollToSlide(int position) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
