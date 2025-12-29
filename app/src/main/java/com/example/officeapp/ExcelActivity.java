package com.example.officeapp;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.officeapp.utils.FileUtils;
import com.google.android.material.tabs.TabLayout;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcelActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private TableLayout tableLayout;
    private ProgressBar progressBar;
    private Workbook workbook;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    private java.util.List<TextView> matchViews = new java.util.ArrayList<>();
    private int currentMatchIndex = -1;
    private android.widget.ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayout);
        tableLayout = findViewById(R.id.tableLayout);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            loadWorkbook(uri);
        } else {
            finish();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                displaySheet(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadWorkbook(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                workbook = WorkbookFactory.create(inputStream);
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    setupTabs();
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ExcelActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void setupTabs() {
        if (workbook == null) return;
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            tabLayout.addTab(tabLayout.newTab().setText(workbook.getSheetName(i)));
        }
        if (numberOfSheets > 0) {
            displaySheet(0);
        }
    }

    private void displaySheet(int sheetIndex) {
        tableLayout.removeAllViews();
        if (workbook == null) return;
        
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        
        for (Row row : sheet) {
            TableRow tableRow = new TableRow(this);
            for (Cell cell : row) {
                TextView textView = new TextView(this);
                textView.setPadding(16, 16, 16, 16);
                textView.setBackgroundResource(android.R.drawable.edit_text);
                textView.setText(getCellValue(cell));
                tableRow.addView(textView);
            }
            tableLayout.addView(tableRow);
        }
    }

    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
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
                if (!query.equals(currentQuery)) {
                    currentQuery = query;
                    highlightCells(query);
                } else {
                    navigateSearch(1);
                }
                // searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.equals(currentQuery)) {
                    currentQuery = newText;
                    highlightCells(newText);
                }
                return true;
            }
        });
        
        searchView.setOnCloseListener(() -> {
            highlightCells("");
            currentQuery = "";
            return false;
        });
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
    private void highlightCells(String query) {
        if (tableLayout == null) return;
        
        matchViews.clear();
        currentMatchIndex = -1;
        String queryLower = query.toLowerCase();
        boolean hasQuery = !query.isEmpty();
        
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cellView = row.getChildAt(j);
                    if (cellView instanceof TextView) {
                        TextView textView = (TextView) cellView;
                        String text = textView.getText().toString().toLowerCase();
                        
                        if (hasQuery && text.contains(queryLower)) {
                            matchViews.add(textView);
                            textView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ExcelActivity.this, R.color.search_highlight));
                        } else {
                            textView.setBackgroundResource(android.R.drawable.edit_text);
                        }
                    }
                }
            }
        }
        
        if (!matchViews.isEmpty()) {
            currentMatchIndex = 0;
            // set active
            matchViews.get(0).setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.search_highlight_active));
            scrollToView(matchViews.get(0));
        }
    }
    
    private long lastNavTime = 0;
    
    private void navigateSearch(int direction) {
        if (matchViews.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavTime < 300) { // 300ms debounce
            return;
        }
        lastNavTime = currentTime;
        
        // De-highlight current active (set back to normal match highlight)
        matchViews.get(currentMatchIndex).setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.search_highlight));
        
        int oldIndex = currentMatchIndex;
        currentMatchIndex += direction;
        if (currentMatchIndex >= matchViews.size()) currentMatchIndex = 0;
        if (currentMatchIndex < 0) currentMatchIndex = matchViews.size() - 1;
        
        android.util.Log.d("ExcelActivity", "Navigating: " + oldIndex + " -> " + currentMatchIndex + " (Total: " + matchViews.size() + ")");

        // Highlight new active
        matchViews.get(currentMatchIndex).setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.search_highlight_active));
        scrollToView(matchViews.get(currentMatchIndex));
    }
    
    private android.widget.HorizontalScrollView horizontalScrollView;

    // ... in onCreate ...
    // Note: I will inject the binding in onCreate via a separate chunk or just rely on finding it here if lazy, 
    // but better to bind in onCreate.
    // Let's assume I bind it in onCreate in a separate replace/chunk or I can just findViewById locally if I want to be safe and lazy,
    // but proper way is field.
    
    private void scrollToView(View view) {
        if (view == null) return;
        
        // Ensure we match the structure: TableRow -> TextView
        if (!(view.getParent() instanceof TableRow)) return;
        TableRow row = (TableRow) view.getParent();
        
        // Calculate positions
        int x = view.getLeft() + row.getLeft();
        int y = row.getTop(); // relative to TableLayout
        
        // Find scroll views if not bound (or bind them in onCreate and use fields)
        if (scrollView == null) scrollView = findViewById(R.id.scrollView);
        if (horizontalScrollView == null) horizontalScrollView = findViewById(R.id.horizontalScrollView);
        
        if (scrollView != null && horizontalScrollView != null) {
            // Scroll Vertical
            int targetY = y - (scrollView.getHeight() / 2) + (row.getHeight() / 2);
            scrollView.smoothScrollTo(0, targetY);
            
            // Scroll Horizontal
            int targetX = x - (horizontalScrollView.getWidth() / 2) + (view.getWidth() / 2);
            horizontalScrollView.smoothScrollTo(targetX, 0);
             
            // Also call requestRectangleOnScreen as backup/accessibility
            view.requestRectangleOnScreen(new android.graphics.Rect(0, 0, view.getWidth(), view.getHeight()), true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
