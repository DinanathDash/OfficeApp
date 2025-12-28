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
import com.spire.doc.Document;
import com.spire.doc.FileFormat;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordActivity extends AppCompatActivity {

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

        progressBar = findViewById(R.id.progressBar);
        View webView = findViewById(R.id.webView);
        if (webView != null) webView.setVisibility(View.GONE); // Hide WebView as we aren't using HTML anymore

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            convertAndLoad(uri, fileName);
        } else {
            finish();
        }
    }

    private void convertAndLoad(Uri uri, String originalFileName) {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Cannot open file");

                // 1. Create a cache file for the PDF
                File pdfFile = new File(getCacheDir(), originalFileName + ".pdf");
                
                // 2. Load DOCX using Spire.Doc
                Document document = new Document();
                
                String extension = FileUtils.getFileExtension(this, uri);
                if (extension != null && extension.equalsIgnoreCase("doc")) {
                    document.loadFromStream(inputStream, FileFormat.Doc);
                } else {
                    document.loadFromStream(inputStream, FileFormat.Docx);
                }
                
                // 3. Convert to PDF
                document.saveToFile(pdfFile.getAbsolutePath(), FileFormat.PDF);
                
                document.close();
                inputStream.close();
                
                // 4. Open with PdfActivity
                Uri pdfUri = FileProvider.getUriForFile(
                        this, 
                        getApplicationContext().getPackageName() + ".provider", 
                        pdfFile);

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    openPdfViewer(pdfUri);
                    finish(); 
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(WordActivity.this, "Conversion failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openPdfViewer(Uri pdfUri) {
        Intent intent = new Intent(this, PdfActivity.class);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
