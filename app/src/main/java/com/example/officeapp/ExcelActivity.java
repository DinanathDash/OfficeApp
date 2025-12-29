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
        
        // Basic implementation: Load on main thread for responsiveness if small, 
        // but for large sheets this should be async or paged.
        // For this task, we'll do simple synchronous loading of the view.
        // Note: For very large sheets, this will freeze. Paging is complex.
        
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        
        for (Row row : sheet) {
            TableRow tableRow = new TableRow(this);
            for (Cell cell : row) {
                TextView textView = new TextView(this);
                textView.setPadding(16, 16, 16, 16);
                textView.setBackgroundResource(android.R.drawable.edit_text); // Simple border effect
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
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                highlightCells(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                highlightCells(newText);
                return true;
            }
        });
        return true;
    }

    private void highlightCells(String query) {
        if (tableLayout == null) return;
        String queryLower = query.toLowerCase();
        
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cellView = row.getChildAt(j);
                    if (cellView instanceof TextView) {
                        TextView textView = (TextView) cellView;
                        String text = textView.getText().toString().toLowerCase();
                        
                        if (!query.isEmpty() && text.contains(queryLower)) {
                            textView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ExcelActivity.this, R.color.search_highlight));
                        } else {
                            textView.setBackgroundResource(android.R.drawable.edit_text);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
