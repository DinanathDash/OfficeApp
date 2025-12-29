package com.example.officeapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.officeapp.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TxtActivity extends AppCompatActivity {

    private TextView textView;
    private String fullText = "";

    private java.util.List<Integer> matchIndices = new java.util.ArrayList<>();
    private int currentMatchIndex = -1;
    private android.widget.ScrollView scrollView;

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
        scrollView = findViewById(R.id.scrollView); // Ensure ID exists in XML, checked below
        
        // Initialize Search Bar
        android.view.View searchBar = findViewById(R.id.searchBar);
        android.widget.EditText searchInput = findViewById(R.id.searchInput);
        android.view.View btnNext = findViewById(R.id.btnNext);
        android.view.View btnPrev = findViewById(R.id.btnPrev);
        android.view.View btnClose = findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> {
            searchBar.setVisibility(android.view.View.GONE);
            findViewById(R.id.toolbar).setVisibility(android.view.View.VISIBLE);
            highlightText(""); // Clear highlights
            searchInput.setText("");
            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        });

        btnNext.setOnClickListener(v -> navigateSearch(1, searchInput.getText().toString()));
        btnPrev.setOnClickListener(v -> navigateSearch(-1, searchInput.getText().toString()));

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                highlightText(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER) {
                navigateSearch(1, searchInput.getText().toString());
                return true;
            }
            return false;
        });

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
                });
                
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(TxtActivity.this, "Error reading file", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            findViewById(R.id.toolbar).setVisibility(android.view.View.GONE);
            findViewById(R.id.searchBar).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.searchInput).requestFocus();
            // Show keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(findViewById(R.id.searchInput), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void highlightText(String query) {
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
                spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.search_highlight)), 
                        index, index + query.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = lowerCaseText.indexOf(lowerCaseQuery, index + query.length());
            }
            
            if (!matchIndices.isEmpty()) {
                currentMatchIndex = 0;
                spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.search_highlight_active)), 
                        matchIndices.get(0), matchIndices.get(0) + query.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                scrollToMatch(matchIndices.get(0));
            }
        }
        textView.setText(spannableString);
    }

    private void navigateSearch(int direction, String query) {
        if (matchIndices.isEmpty()) return;
        
        currentMatchIndex += direction;
        if (currentMatchIndex >= matchIndices.size()) currentMatchIndex = 0;
        if (currentMatchIndex < 0) currentMatchIndex = matchIndices.size() - 1;
        
        // Re-apply spans (inefficient but safe for simple highlights)
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        int qLen = query.length();
        
        for (int i = 0; i < matchIndices.size(); i++) {
            int idx = matchIndices.get(i);
            int color = (i == currentMatchIndex) ? R.color.search_highlight_active : R.color.search_highlight;
            spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(this, color)), 
                    idx, idx + qLen, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        textView.setText(spannableString);
        scrollToMatch(matchIndices.get(currentMatchIndex));
    }
    
    private void scrollToMatch(int index) {
        if (textView.getLayout() != null) {
            int line = textView.getLayout().getLineForOffset(index);
            int y = textView.getLayout().getLineTop(line);
            if (scrollView != null) {
                scrollView.smoothScrollTo(0, y);
            }
        }
    }
}
