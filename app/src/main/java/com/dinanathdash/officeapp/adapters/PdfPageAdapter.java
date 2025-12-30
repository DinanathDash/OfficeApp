package com.dinanathdash.officeapp.adapters;

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

import com.dinanathdash.officeapp.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {

    private PdfRenderer pdfRenderer;
    private int pageCount;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private OnPageLongClickListener longClickListener;

    public interface OnPageLongClickListener {
        void onPageLongClick(int pageIndex);
    }

    public void setOnPageLongClickListener(OnPageLongClickListener listener) {
        this.longClickListener = listener;
    }

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
        holder.imageView.setImageBitmap(null); 
        holder.imageView.setAdjustViewBounds(false); 
        // Force minimum size to prevent 0x0 collapse with UNSPECIFIED measurements
        holder.imageView.setMinimumWidth(1080);
        holder.imageView.setMinimumHeight(1400);
        
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPageLongClick(holder.getAdapterPosition());
                return true;
            }
            return false;
        });

        // Render Async
        executorService.execute(() -> {
            Bitmap bitmap = renderPage(position);
            if (bitmap != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                   if (holder.getAdapterPosition() == position) {
                       holder.imageView.setAdjustViewBounds(true); // Enable adjustBounds for correct aspect ratio
                       holder.imageView.setImageBitmap(bitmap);
                   }
                });
            }
        });
    }
    
    // PdfRenderer is not thread-safe, strict synchronization needed
    private synchronized Bitmap renderPage(int index) {
        if (pdfRenderer == null) {
            android.util.Log.e("PdfPageAdapter", "RenderPage: pdfRenderer is null for page " + index);
            return null;
        }
        PdfRenderer.Page page = null;
        try {
            page = pdfRenderer.openPage(index);
            // High quality rendering: scale relative to screen usually, here simply simplified 
            int width = 1080; // Fixed width for simplification, ideally get screen width
            int height = width * page.getHeight() / page.getWidth();
            
            android.util.Log.d("PdfPageAdapter", "RenderPage " + index + ": width=" + width + ", height=" + height);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(android.graphics.Color.WHITE);
            
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            float scale = (float) width / page.getWidth();
            matrix.postScale(scale, scale);
            
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            android.util.Log.d("PdfPageAdapter", "RenderPage " + index + ": Success");
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PdfPageAdapter", "RenderPage " + index + ": Failed", e);
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
            com.dinanathdash.officeapp.ui.ZoomLayout zoomLayout = itemView.findViewById(R.id.zoomLayout);
            if (zoomLayout != null) {
                zoomLayout.setScrollableAtScaleOne(false); // Let RecyclerView handle scrolling when not zoomed
                zoomLayout.setMeasureMode(com.dinanathdash.officeapp.ui.ZoomLayout.MeasureMode.UNBOUNDED_BOTH); // Allow image to be full resolution/size
            }
            pageNumber = itemView.findViewById(R.id.pdfPageNumber);
        }
    }
}
