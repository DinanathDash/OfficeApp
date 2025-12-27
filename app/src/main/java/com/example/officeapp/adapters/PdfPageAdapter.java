package com.example.officeapp.adapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officeapp.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {

    private PdfRenderer pdfRenderer;
    private int pageCount;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    public PdfPageAdapter(PdfRenderer pdfRenderer) {
        this.pdfRenderer = pdfRenderer;
        this.pageCount = pdfRenderer.getPageCount();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.pageNumber.setText("Page " + (position + 1));
        holder.imageView.setImageBitmap(null); // Clear previous

        // Render Async
        executorService.execute(() -> {
            Bitmap bitmap = renderPage(position);
            if (bitmap != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                   if (holder.getAdapterPosition() == position) {
                       holder.imageView.setImageBitmap(bitmap);
                   }
                });
            }
        });
    }
    
    // PdfRenderer is not thread-safe, strict synchronization needed
    private synchronized Bitmap renderPage(int index) {
        if (pdfRenderer == null) return null;
        PdfRenderer.Page page = null;
        try {
            page = pdfRenderer.openPage(index);
            // High quality rendering: scale relative to screen usually, here simply simplified 
            int width = 1080; // Fixed width for simplification, ideally get screen width
            int height = width * page.getHeight() / page.getWidth();
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }

    @Override
    public int getItemCount() {
        return pageCount;
    }
    
    public void close() {
        executorService.shutdown();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView pageNumber;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pdfPageImage);
            pageNumber = itemView.findViewById(R.id.pdfPageNumber);
        }
    }
}
