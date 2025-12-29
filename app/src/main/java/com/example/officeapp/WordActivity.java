package com.example.officeapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.example.officeapp.utils.FileUtils;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private android.webkit.WebView webView;
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

        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);
        
        // Configure WebView
        if (webView != null) {
            webView.setVisibility(View.GONE);
            webView.getSettings().setJavaScriptEnabled(true); // Enable JS for search
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false); // Hide zoom controls
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
        }

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            loadDocxToWebView(uri);
        } else {
            finish();
        }
    }

    private String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
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
                // Trigger next match on Enter
                if (webView != null) {
                    if (!query.equals(currentQuery)) {
                        currentQuery = query;
                        webView.evaluateJavascript("highlightText('" + escapeJsString(query) + "')", null);
                    } else {
                        webView.evaluateJavascript("navigateSearch(1)", null);
                    }
                }
                // searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Determine if we should highlight on change or wait for submit
                // For now, let's keep it responsive but update currentQuery if we want strict mode
                // But performSearch resets index, so we should allow it for "live" search, 
                // but when Enter is pressed, we don't want to reset if it's the same text.
                if (!newText.equals(currentQuery)) {
                    currentQuery = newText;
                    performSearch(newText);
                }
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            if (webView != null) webView.evaluateJavascript("highlightText('')", null);
            currentQuery = "";
            return false;
        });

        return true;
    }

    private void performSearch(String query) {
        if (webView != null) {
            String escapedQuery = escapeJsString(query);
            webView.evaluateJavascript("highlightText('" + escapedQuery + "')", null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void loadDocxToWebView(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Cannot open file");

                // Load DOCX with Apache POI
                org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream);

                // Convert to HTML
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<html><head><style>")
                          .append("body { font-family: sans-serif; padding: 16px; }")
                          .append("table { border-collapse: collapse; width: 100%; border: 1px solid #ccc; }")
                          .append("td, th { border: 1px solid #ccc; padding: 8px; }")
                          .append("img { max-width: 100%; height: auto; }")
                          .append(".search-highlight { background-color: #ABD1FF; color: black; }") // Theme light blue
                          .append(".search-highlight-active { background-color: #0066FF; color: white; }") // Theme primary blue
                          .append("</style>")
                          .append("<script>")
                          .append("var currentMatchIndex = 0;")
                          .append("function escapeHtml(text) {")
                          .append("  return text.replace(/&/g, '&amp;')")
                          .append("             .replace(/</g, '&lt;')")
                          .append("             .replace(/>/g, '&gt;')")
                          .append("             .replace(/\"/g, '&quot;')")
                          .append("             .replace(/'/g, '&#039;');")
                          .append("}")
                          .append("function highlightText(query) {")
                          .append("  currentMatchIndex = 0;")
                          .append("  // Remove old highlights\n")
                          .append("  var oldSpans = document.querySelectorAll('span.search-highlight');\n")
                          .append("  for (var i = 0; i < oldSpans.length; i++) {\n")
                          .append("    var parent = oldSpans[i].parentNode;\n")
                          .append("    parent.replaceChild(document.createTextNode(oldSpans[i].textContent), oldSpans[i]);\n")
                          .append("    parent.normalize();\n")
                          .append("  }\n")
                          .append("  if (!query) return 0;\n")
                          .append("  \n")
                          .append("  // Find and highlight new\n")
                          .append("  var regex = new RegExp('(' + query.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&') + ')', 'gi');\n")
                          .append("  var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);\n")
                          .append("  var nodeList = [];\n")
                          .append("  while(walker.nextNode()) nodeList.push(walker.currentNode);\n")
                          .append("  \n")
                          .append("  for (var i = 0; i < nodeList.length; i++) {\n")
                          .append("    var node = nodeList[i];\n")
                          .append("    if (node.parentNode.tagName === 'SCRIPT' || node.parentNode.tagName === 'STYLE') continue;\n")
                          .append("    if (regex.test(node.nodeValue)) {\n")
                          .append("      var span = document.createElement('span');\n")
                          .append("      var safeText = escapeHtml(node.nodeValue);\n")
                          .append("      span.innerHTML = safeText.replace(regex, '<span class=\"search-highlight\">$1</span>');\n")
                          .append("      node.parentNode.replaceChild(span, node);\n")
                          .append("    }\n")
                          .append("  }\n")
                          .append("  // Scroll to first match (active)\n")
                          .append("  var matches = document.querySelectorAll('.search-highlight');\n")
                          .append("  if (matches.length > 0) {\n")
                          .append("    matches[0].classList.add('search-highlight-active');\n")
                          .append("    matches[0].scrollIntoView({behavior: 'smooth', block: 'center'});\n")
                          .append("  }\n")
                          .append("  return matches.length;\n")
                          .append("}")
                          .append("var lastNavTime = 0;")
                          .append("function navigateSearch(direction) {")
                          .append("  var now = Date.now();")
                          .append("  if (now - lastNavTime < 300) return;")
                          .append("  lastNavTime = now;")
                          .append("  var matches = document.querySelectorAll('.search-highlight');")
                          .append("  if (matches.length === 0) return;")
                          .append("  matches[currentMatchIndex].classList.remove('search-highlight-active');")
                          .append("  currentMatchIndex += direction;")
                          .append("  if (currentMatchIndex >= matches.length) currentMatchIndex = 0;")
                          .append("  if (currentMatchIndex < 0) currentMatchIndex = matches.length - 1;")
                          .append("  matches[currentMatchIndex].classList.add('search-highlight-active');")
                          .append("  matches[currentMatchIndex].scrollIntoView({behavior: 'smooth', block: 'center'});")
                          .append("}")
                          .append("</script>")
                          .append("</head><body>");

                for (org.apache.poi.xwpf.usermodel.IBodyElement element : document.getBodyElements()) {
                    if (element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph) {
                        org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = (org.apache.poi.xwpf.usermodel.XWPFParagraph) element;
                        htmlBuilder.append("<p>");
                        for (org.apache.poi.xwpf.usermodel.XWPFRun run : paragraph.getRuns()) {
                            // Text
                            String text = run.getText(0);
                            if (text != null) {
                                if (run.isBold()) htmlBuilder.append("<b>");
                                if (run.isItalic()) htmlBuilder.append("<i>");
                                htmlBuilder.append(text.replace("<", "&lt;").replace(">", "&gt;"));
                                if (run.isItalic()) htmlBuilder.append("</i>");
                                if (run.isBold()) htmlBuilder.append("</b>");
                            }

                            // Images
                            for (org.apache.poi.xwpf.usermodel.XWPFPicture picture : run.getEmbeddedPictures()) {
                                org.apache.poi.xwpf.usermodel.XWPFPictureData data = picture.getPictureData();
                                byte[] bytes = data.getData();
                                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                                // Determine mime type roughly or assume png/jpeg
                                String mimeType = "image/png"; 
                                String ext = data.getFileName().substring(data.getFileName().lastIndexOf(".") + 1).toLowerCase();
                                if (ext.equals("jpg") || ext.equals("jpeg")) mimeType = "image/jpeg";
                                
                                htmlBuilder.append("<img src=\"data:" + mimeType + ";base64," + base64 + "\"/>");
                            }
                        }
                        htmlBuilder.append("</p>");
                    } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                        org.apache.poi.xwpf.usermodel.XWPFTable table = (org.apache.poi.xwpf.usermodel.XWPFTable) element;
                        htmlBuilder.append("<table>");
                        for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                            htmlBuilder.append("<tr>");
                            for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                                htmlBuilder.append("<td>").append(cell.getText().replace("<", "&lt;").replace(">", "&gt;")).append("</td>");
                            }
                            htmlBuilder.append("</tr>");
                        }
                        htmlBuilder.append("</table>");
                    }
                }
                
                htmlBuilder.append("</body></html>");
                String htmlContent = htmlBuilder.toString();
                
                document.close();
                inputStream.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (webView != null) {
                        webView.setVisibility(View.VISIBLE);
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(WordActivity.this, "Error loading document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
