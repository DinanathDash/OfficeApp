package com.example.officeapp;

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

import com.example.officeapp.adapters.SlidesAdapter;
import com.example.officeapp.data.Slide;
import com.example.officeapp.utils.FileUtils;

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

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(slides);
                progressBar.setVisibility(View.GONE);
                if(getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(slides.size() + " Slides");
                }
                
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
