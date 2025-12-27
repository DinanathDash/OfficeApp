package com.example.officeapp;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.officeapp.utils.FileUtils;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;
// Note: XWPFPicture handling requires more complex setup, simplified here

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // Configure WebView
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        // Enable overview mode for A4 simulation
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            loadDocument(uri);
        } else {
            finish();
        }
    }

    private void loadDocument(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        // Load blank initially
        webView.loadData("", "text/html", "UTF-8");

        executorService.execute(() -> {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta name='viewport' content='width=1000'><style>")
                .append("body { font-family: sans-serif; background-color: #F0F0F0; color: #333; width: 100%; margin: 0; padding: 0; }")
                .append(".page { background-color: white; width: 1000px; margin: 20px auto; padding: 40px; box-shadow: 0 0 10px rgba(0,0,0,0.1); min-height: 1000px; box-sizing: border-box; }")
                .append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }")
                .append("td, th { border: 1px solid #999; padding: 4px; }")
                .append("img { max-width: 100%; height: auto; }")
                .append("p { margin: 8px 0; }")
                .append("</style></head><body>");

                // Start the first page
                html.append("<div class='page'>");

            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Cannot open file");

                String extension = FileUtils.getFileExtension(this, uri);
                
                if (extension != null && (extension.equalsIgnoreCase("docx"))) {
                    html.append(convertDocxToHtml(inputStream));
                } else if (extension != null && (extension.equalsIgnoreCase("doc"))) {
                    html.append(convertDocToHtml(inputStream));
                } else {
                    html.append("<p>Preview unavailable for this format.</p>");
                }
                
                html.append("</div></body></html>");
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                html.setLength(0);
                html.append("<html><body><p style='color:red'>Error loading file: ").append(e.getMessage()).append("</p></body></html>");
            }

            String finalHtml = html.toString();
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                webView.loadDataWithBaseURL(null, finalHtml, "text/html", "UTF-8", null);
            });
        });
    }

    private String convertDocxToHtml(InputStream inputStream) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder sb = new StringBuilder();

        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                processParagraph((XWPFParagraph) element, sb, false);
            } else if (element instanceof XWPFTable) {
                processTable((XWPFTable) element, sb);
            }
        }
        document.close();
        return sb.toString();
    }

    private void processParagraph(XWPFParagraph paragraph, StringBuilder sb, boolean inTable) {
        if (!inTable && paragraph.isPageBreak()) {
            sb.append("</div><div class='page'>");
        }

        sb.append("<p style='");
        
        // Alignment
        if (paragraph.getAlignment() != null) {
            sb.append("text-align:").append(paragraph.getAlignment().toString().toLowerCase()).append(";");
        }
        
        // Indentation
        int indentationLeft = paragraph.getIndentationLeft();
        if (indentationLeft > 0) {
            // POI uses twips (1/20th of a point). Approx conversion: 1440 twips = 1 inch
            sb.append("margin-left:").append(indentationLeft / 20).append("pt;");
        }
        
        int indentationRight = paragraph.getIndentationRight();
        if (indentationRight > 0) {
            sb.append("margin-right:").append(indentationRight / 20).append("pt;");
        }

        // Spacing
        int spacingBefore = paragraph.getSpacingBefore();
        if (spacingBefore > 0) {
            sb.append("margin-top:").append(spacingBefore / 20).append("pt;");
        }
        
        int spacingAfter = paragraph.getSpacingAfter();
        if (spacingAfter > 0) {
            sb.append("margin-bottom:").append(spacingAfter / 20).append("pt;");
        }
        
        sb.append("'>");

        for (XWPFRun run : paragraph.getRuns()) {
            // Check for Page Break
            if (!inTable) {
                List<CTBr> brList = run.getCTR().getBrList();
                for (CTBr br : brList) {
                    if (br.getType() != null && br.getType().equals(STBrType.PAGE)) {
                        // Close current paragraph (if open - simplified here assuming we are at start or handle split inside)
                        // Actually, to keep it simple, if we hit a page break, we close the current page div and start a new one.
                        // We must be careful if we are inside a <p>.
                        // A strictly correct HTML implementation would require closing the <p>, closing the </div>, opening <div>, opening <p>.
                        // But since we are iterating runs, we are inside <p>.
                        sb.append("</p></div><div class='page'><p style='");
                        // Re-apply style for the new paragraph segment (simplified)
                         if (paragraph.getAlignment() != null) {
                            sb.append("text-align:").append(paragraph.getAlignment().toString().toLowerCase()).append(";");
                        }
                        sb.append("'>");
                    }
                }
            }

            // Basic Formatting
            if (run.isBold()) sb.append("<b>");
            if (run.isItalic()) sb.append("<i>");
            if (run.isStrikeThrough()) sb.append("<strike>");
            
             // Colors
            String color = run.getColor();
            if (color != null) {
                sb.append("<span style='color:#").append(color).append("'>");
            }
            
            // Font Size
            int fontSize = run.getFontSize();
            if (fontSize > 0) {
                 if (color == null) sb.append("<span style='");
                 else sb.append(";");
                 sb.append("font-size:").append(fontSize).append("pt'>");
            }

            // Images
            if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
                 for (org.apache.poi.xwpf.usermodel.XWPFPicture pic : run.getEmbeddedPictures()) {
                     byte[] data = pic.getPictureData().getData();
                     String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
                     sb.append("<img src=\"data:image/png;base64,").append(base64).append("\"/>");
                 }
            }
            
            String text = run.getText(0);
            if (text != null) {
                // Handling new lines
                text = text.replace("\n", "<br>");
                sb.append(text);
            }
            
            if (fontSize > 0 || color != null) sb.append("</span>");
            if (run.isStrikeThrough()) sb.append("</strike>");
            if (run.isItalic()) sb.append("</i>");
            if (run.isBold()) sb.append("</b>");
        }
        sb.append("</p>");
    }

    private void processTable(XWPFTable table, StringBuilder sb) {
        sb.append("<table>");
        for (XWPFTableRow row : table.getRows()) {
            sb.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append("<td>");
                // Cells contain paragraphs
                for (XWPFParagraph p : cell.getParagraphs()) {
                    processParagraph(p, sb, true);
                }
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    private String convertDocToHtml(InputStream inputStream) throws Exception {
        // HWPF is older and harder to convert perfectly to HTML structurally
        // We will try extracting paragraphs and rudimentary conversion
        HWPFDocument document = new HWPFDocument(inputStream);
        Range range = document.getRange();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < range.numParagraphs(); i++) {
            Paragraph p = range.getParagraph(i);
            sb.append("<p>");
            // CharacterRun extraction is needed for bold/images in HWPF, which is complex
            // Basic text fall back for .doc
            sb.append(p.text().replace("\n", "<br>"));
            sb.append("</p>");
        }
        document.close();
        return sb.toString();
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                findAll(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Finding while typing in WebView is a bit jumpy, usually submit is better or findNext
                // findAll highlights all.
                findAll(newText);
                return true;
            }
        });
        return true;
    }
    
    private void findAll(String query) {
        if (webView != null) {
             // WebView findAllAsync verifies availability
             webView.findAllAsync(query);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
