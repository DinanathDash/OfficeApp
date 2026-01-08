package com.dinanathdash.officeapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dinanathdash.officeapp.utils.ViewUtils;

import com.dinanathdash.officeapp.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TxtActivity extends AppCompatActivity {

    private TextView textView;
    private String fullText = "";
    private com.airbnb.lottie.LottieAnimationView progressBar;

    private java.util.List<Integer> matchIndices = new java.util.ArrayList<>();
    private int currentMatchIndex = -1;
    private com.dinanathdash.officeapp.ui.ZoomLayout zoomLayout; // NEW

    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());

        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);
        
        if (progressBar != null) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            com.dinanathdash.officeapp.utils.LoaderUtils.applyThemeColors(progressBar, primaryColor);
        }

        zoomLayout = findViewById(R.id.zoomLayout);
        zoomLayout.setMeasureMode(com.dinanathdash.officeapp.ui.ZoomLayout.MeasureMode.UNBOUNDED_VERTICAL); // Vertical scroll, width constrained for wrap
        zoomLayout.setScrollableAtScaleOne(true); // Handle scrolling ourselves
        ViewUtils.applyBottomWindowInsets(zoomLayout);
        


        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(fileName);
            }
            loadText(uri);
        } else {
            finish();
        }
    }

    private void loadText(Uri uri) {
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> {
            StringBuilder stringBuilder = new StringBuilder();
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                
                String text = stringBuilder.toString();
                runOnUiThread(() -> {
                    fullText = text;
                    textView.setText(fullText);
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                });
                
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(TxtActivity.this, "Error reading file", Toast.LENGTH_SHORT).show();
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // When user presses enter, go to next match
                // If it's a new query (different from current), highlightText will reset indices
                // If it's same query, we just navigate
                if (!query.equals(currentQuery)) {
                    highlightText(query);
                } else {
                    navigateSearch(1);
                }
                // searchView.clearFocus(); // Removed to allow repeated Enter presses
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                highlightText(newText);
                return true;
            }
        });
        
        searchView.setOnCloseListener(() -> {
            highlightText("");
            return false;
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void highlightText(String query) {
        currentQuery = query;
        matchIndices.clear();
        currentMatchIndex = -1;
        
        if (fullText.isEmpty()) return;

        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        if (!query.isEmpty()) {
            String lowerCaseText = fullText.toLowerCase();
            String lowerCaseQuery = query.toLowerCase();
            int index = lowerCaseText.indexOf(lowerCaseQuery);
            
            while (index >= 0) {
                matchIndices.add(index);
                spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.txt_highlight)), 
                        index, index + query.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = lowerCaseText.indexOf(lowerCaseQuery, index + query.length());
            }
            
            if (!matchIndices.isEmpty()) {
                currentMatchIndex = 0;
                spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.txt_highlight_active)), 
                        matchIndices.get(0), matchIndices.get(0) + query.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                scrollToMatch(matchIndices.get(0));
            }
        }
        
        textView.setText(spannableString);
    }

    private long lastNavTime = 0;

    private void navigateSearch(int direction) {
        if (matchIndices.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavTime < 300) { // 300ms debounce
            return;
        }
        lastNavTime = currentTime;
        
        currentMatchIndex += direction;
        if (currentMatchIndex >= matchIndices.size()) currentMatchIndex = 0;
        if (currentMatchIndex < 0) currentMatchIndex = matchIndices.size() - 1;
        
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        int qLen = currentQuery.length();
        
        // Re-highlight all
        for (int idx : matchIndices) {
             spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.txt_highlight)), 
                    idx, idx + qLen, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // Highlight active
        int activeIdx = matchIndices.get(currentMatchIndex);
        spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.txt_highlight_active)), 
                activeIdx, activeIdx + qLen, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(spannableString);
        scrollToMatch(activeIdx);
    }
    

    
    private void scrollToMatch(int index) {
        if (textView.getLayout() != null) {
            int line = textView.getLayout().getLineForOffset(index);
            int y = textView.getLayout().getLineTop(line);
            
            if (zoomLayout == null) zoomLayout = findViewById(R.id.zoomLayout);
            if (zoomLayout != null) {
                // Not easily scrollable via simple API in ZoomLayout yet
                 // Calculate layout position
                 float centerX = textView.getWidth() / 2f; 
                 float centerY = y; // Top of the line
                 
                 zoomLayout.scrollToTopArea(centerX, centerY, true);
            }
        }
    }
}
