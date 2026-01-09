package com.dinanathdash.officeapp;

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

import com.dinanathdash.officeapp.utils.FileUtils;
import com.google.android.material.tabs.TabLayout;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
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
    private com.airbnb.lottie.LottieAnimationView progressBar;
    private Workbook workbook;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    private java.util.List<TextView> matchViews = new java.util.ArrayList<>();
    private int currentMatchIndex = -1;
    private com.dinanathdash.officeapp.ui.ZoomLayout zoomLayout; // NEW
    
    // Incremental Rendering Locals
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRenderTask;
    private static final int BATCH_SIZE = 30; // Rows to render per frame/tick
    
    // Cache for border color to avoid potential resource lookup every cell
    private static final int BORDER_COLOR = 0xFFE0E0E0; 
    private static final int BORDER_WIDTH_PX = 2; // approx 1dp



    private Integer getBackgroundColorFromStyle(org.apache.poi.ss.usermodel.CellStyle style) {
        if (style == null) return null;

        // Check for fill pattern, if NO_FILL, return null
        if (style.getFillPattern() == org.apache.poi.ss.usermodel.FillPatternType.NO_FILL) return null;

        try {
            org.apache.poi.ss.usermodel.Color color = style.getFillForegroundColorColor();
            if (color instanceof org.apache.poi.xssf.usermodel.XSSFColor) {
                byte[] rgb = ((org.apache.poi.xssf.usermodel.XSSFColor) color).getRGB();
                if (rgb != null) {
                   return Color.rgb(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                }
            } else if (color instanceof org.apache.poi.hssf.util.HSSFColor) {
                short[] triplet = ((org.apache.poi.hssf.util.HSSFColor) color).getTriplet();
                if (triplet != null) {
                    return Color.rgb(triplet[0], triplet[1], triplet[2]);
                }
            } 
            
            // Fallback for indexed colors if color object is null or not handled above
            if (workbook instanceof org.apache.poi.hssf.usermodel.HSSFWorkbook) {
                 short fillIndex = style.getFillForegroundColor();
                 org.apache.poi.hssf.usermodel.HSSFPalette palette = ((org.apache.poi.hssf.usermodel.HSSFWorkbook) workbook).getCustomPalette();
                 org.apache.poi.hssf.util.HSSFColor hssfColor = palette.getColor(fillIndex);
                 if (hssfColor != null) {
                     short[] triplet = hssfColor.getTriplet();
                      return Color.rgb(triplet[0], triplet[1], triplet[2]);
                 }
            }
        } catch (Exception e) {
            // Ignore color extraction errors, return null
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        
        // Configure light status bar icons for colored toolbar
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
            .setAppearanceLightStatusBars(false);
        
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

        // Apply Dynamic Colors
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int primaryColor = typedValue.data;
        com.dinanathdash.officeapp.utils.LoaderUtils.applyThemeColors(progressBar, primaryColor);

        zoomLayout = findViewById(R.id.zoomLayout);
        zoomLayout.setMeasureMode(com.dinanathdash.officeapp.ui.ZoomLayout.MeasureMode.UNBOUNDED_BOTH);
        
        // Setup bottom navigation spacer
        View bottomSpacer = findViewById(R.id.bottomNavSpacer);
        com.dinanathdash.officeapp.utils.BottomNavHelper.setupBottomSpacer(bottomSpacer);
        

        


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
        // Cancel any existing render task to prevent race conditions or double rendering
        if (pendingRenderTask != null) {
            mainHandler.removeCallbacks(pendingRenderTask);
            pendingRenderTask = null;
        }

        tableLayout.removeAllViews();
        if (workbook == null) return;
        
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        DataFormatter formatter = new DataFormatter();
        
        // 1. Calculate max column count across ALL rows to ensure rectangular grid
        // (This might still be slow for huge sheets, but much faster than inflating views. 
        //  Ideally this should be cached or done bg, but let's keep it simple for now as view inflation is the bottleneck)
        int maxColCount = 0;
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {
             Row row = sheet.getRow(i);
             if (row != null) {
                 int lastCell = row.getLastCellNum();
                 if (lastCell > maxColCount) maxColCount = lastCell;
             }
        }
        
        // 2. Render Header Row IMMEDIATELY
        tableLayout.setBackgroundColor(Color.parseColor("#E0E0E0")); 
        
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#E0E0E0")); 
        
        // Corner cell
        TextView cornerView = new TextView(this);
        cornerView.setText("");
        cornerView.setBackgroundColor(Color.parseColor("#F5F5F5")); 
        TableRow.LayoutParams cornerParams = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT
        );
        cornerParams.setMargins(1, 1, 1, 1);
        cornerView.setLayoutParams(cornerParams);
        cornerView.setPadding(24, 16, 24, 16);
        headerRow.addView(cornerView);
        
        for(int c=0; c<maxColCount; c++) {
            TextView colHeader = new TextView(this);
            colHeader.setText(getColumnName(c));
            colHeader.setGravity(android.view.Gravity.CENTER);
            colHeader.setBackgroundColor(Color.parseColor("#F5F5F5"));
            colHeader.setTextColor(Color.BLACK);
            colHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            
            TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            );
            params.setMargins(0, 1, 1, 1); 
            colHeader.setLayoutParams(params);
            colHeader.setPadding(24, 16, 24, 16);
            
            headerRow.addView(colHeader);
        }
        tableLayout.addView(headerRow);
        
        // 3. Start Incremental Rendering for Data Rows
        progressBar.setVisibility(View.VISIBLE); // Ensure loader is visible
        renderRowsIncrementally(sheet, 0, lastRowNum, maxColCount, formatter);
    }

    private void renderRowsIncrementally(Sheet sheet, int startRow, int lastRowNum, int maxColCount, DataFormatter formatter) {
        int endRow = Math.min(startRow + BATCH_SIZE, lastRowNum + 1);
        
        for (int r = startRow; r < endRow; r++) {
            Row row = sheet.getRow(r);
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundColor(Color.parseColor("#E0E0E0"));
            
            // Row Header
            TextView rowHeader = new TextView(this);
            rowHeader.setText(String.valueOf(r + 1));
            TableRow.LayoutParams headerParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            );
            headerParams.setMargins(1, 0, 1, 1);
            rowHeader.setLayoutParams(headerParams);
            rowHeader.setGravity(android.view.Gravity.CENTER);
            rowHeader.setBackgroundColor(Color.parseColor("#F5F5F5"));
            rowHeader.setTextColor(Color.BLACK);
            rowHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            rowHeader.setPadding(24, 16, 24, 16);
            tableRow.addView(rowHeader);
            
            // Data Cells
            for (int c = 0; c < maxColCount; c++) {
                Cell cell = (row != null) ? row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK) : null;
                
                TextView textView = new TextView(this);
                TableRow.LayoutParams cellParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.MATCH_PARENT
                );
                cellParams.setMargins(0, 0, 1, 1);
                textView.setLayoutParams(cellParams);
                textView.setPadding(24, 16, 24, 16);
                
                textView.setBackgroundColor(Color.WHITE);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(14f);
                textView.setGravity(android.view.Gravity.CENTER_VERTICAL);
                textView.setTextIsSelectable(true);
                
                String text = "";
                Integer bgColor = null;

                if (cell != null) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                        try {
                            switch (cell.getCachedFormulaResultType()) {
                                case NUMERIC:
                                    text = formatter.formatRawCellContents(
                                            cell.getNumericCellValue(),
                                            cell.getCellStyle().getDataFormat(),
                                            cell.getCellStyle().getDataFormatString()
                                    );
                                    break;
                                case STRING:
                                    text = cell.getStringCellValue();
                                    break;
                                case BOOLEAN:
                                    text = String.valueOf(cell.getBooleanCellValue());
                                    break;
                                case ERROR:
                                    text = org.apache.poi.ss.usermodel.FormulaError.forInt(cell.getErrorCellValue()).getString();
                                    break;
                                default:
                                    text = formatter.formatCellValue(cell);
                            }
                        } catch (Exception e) {
                            text = formatter.formatCellValue(cell);
                        }
                    } else {
                        text = formatter.formatCellValue(cell);
                    }
                    bgColor = getBackgroundColorFromStyle(cell.getCellStyle());
                }
                
                if (bgColor == null && row != null) {
                     bgColor = getBackgroundColorFromStyle(row.getRowStyle());
                }

                if (bgColor != null) {
                    textView.setBackgroundColor(bgColor);
                }
                
                textView.setText(text);
                tableRow.addView(textView);
            }
            tableLayout.addView(tableRow);
        }
        
        // Schedule next batch or finish
        if (endRow <= lastRowNum) {
            pendingRenderTask = () -> renderRowsIncrementally(sheet, endRow, lastRowNum, maxColCount, formatter);
            mainHandler.post(pendingRenderTask);
        } else {
            // Done!
            progressBar.setVisibility(View.GONE);
            pendingRenderTask = null;
        }
    }

    private String getColumnName(int index) {
        StringBuilder sb = new StringBuilder();
        while (index >= 0) {
            sb.insert(0, (char) ('A' + index % 26));
            index = index / 26 - 1;
        }
        return sb.toString();
    }
    
    private String extractSheetText(int sheetIndex) {
        if (workbook == null) return "";
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {
             Row row = sheet.getRow(i);
             if (row != null) {
                 boolean firstCell = true;
                 for (int c = 0; c < row.getLastCellNum(); c++) {
                     Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                     if (!firstCell) sb.append("\t");
                     if (cell != null) {
                         sb.append(formatter.formatCellValue(cell));
                     }
                     firstCell = false;
                 }
             }
             sb.append("\n");
        }
        return sb.toString();
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
                            textView.setBackgroundResource(R.drawable.excel_highlight_bg);
                        } else {
                            textView.setBackgroundResource(R.drawable.table_cell_bg);
                        }
                    }
                }
            }
        }
        
        if (!matchViews.isEmpty()) {
            currentMatchIndex = 0;
            // set active
            matchViews.get(0).setBackgroundResource(R.drawable.excel_highlight_active_bg);
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
        matchViews.get(currentMatchIndex).setBackgroundResource(R.drawable.excel_highlight_bg);
        
        int oldIndex = currentMatchIndex;
        currentMatchIndex += direction;
        if (currentMatchIndex >= matchViews.size()) currentMatchIndex = 0;
        if (currentMatchIndex < 0) currentMatchIndex = matchViews.size() - 1;
        
        android.util.Log.d("ExcelActivity", "Navigating: " + oldIndex + " -> " + currentMatchIndex + " (Total: " + matchViews.size() + ")");

        // Highlight new active
        matchViews.get(currentMatchIndex).setBackgroundResource(R.drawable.excel_highlight_active_bg);
        scrollToView(matchViews.get(currentMatchIndex));
    }
    


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
        
        // Calculate positions relative to TableLayout
        // view.getLeft() is relative to Row. row.getLeft() is relative to Table.
        int x = view.getLeft() + row.getLeft();
        int y = row.getTop(); // TableRow top relative to Table
        
        // Add view's own top/left offsets if necessary (e.g. padding/margins effectively, but getLeft/Top handles position)
        // Usually view.getTop() inside a TableRow is 0 or small vertical alignment offset.
        y += view.getTop();

        // Calculate center of the target view
        float centerX = x + view.getWidth() / 2f;
        float centerY = y + view.getHeight() / 2f;
        
        if (zoomLayout == null) zoomLayout = findViewById(R.id.zoomLayout);
        
        if (zoomLayout != null) {
             zoomLayout.scrollToTopArea(centerX, centerY, true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
