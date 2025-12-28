package com.example.officeapp;

import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.view.ViewGroup;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officeapp.adapters.PdfPageAdapter;
import com.example.officeapp.utils.FileUtils;

import java.io.IOException;

public class PdfActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PdfPageAdapter adapter;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            try {
                openRenderer(uri);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void openRenderer(Uri uri) throws IOException {
        fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        if (fileDescriptor != null) {
            pdfRenderer = new PdfRenderer(fileDescriptor);
            adapter = new PdfPageAdapter(pdfRenderer);
            recyclerView.setAdapter(adapter);

            if (pdfRenderer.getPageCount() == 1) {
                // If single page, center vertically (WRAP_CONTENT in a constrained layout centers if constraints are set)
                // ConstraintLayout centers the view if it is wrap_content and constraints are Top/Bottom of parent
                ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                recyclerView.setLayoutParams(params);
            } else {
                 ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT; // Or 0dp (match_constraint) depending on setup
                recyclerView.setLayoutParams(params);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) {
            adapter.close();
        }
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
