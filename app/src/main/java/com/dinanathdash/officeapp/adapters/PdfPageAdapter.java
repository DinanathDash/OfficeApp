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

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.dinanathdash.officeapp.utils.PdfHighlighter;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dinanathdash.officeapp.ui.GlobalZoomHelper;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> implements GlobalZoomHelper.ZoomableAdapter {

    private PdfRenderer pdfRenderer;
    private int pageCount;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private OnPageLongClickListener longClickListener;
    
    // Search highlight support
    private PDDocument pdDocument;
    private PdfHighlighter highlighter;
    private String searchQuery = "";
    private List<Integer> searchResultsPages;
    private int highlightColor = Color.YELLOW;
    private int activeHighlightColor = Color.RED; // Not fully used per match yet, page level?
    
    private float globalScale = 1.0f;
    
    @Override
    public void setGlobalScale(float scale) {
        this.globalScale = scale;
        notifyDataSetChanged();
    }

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

    public void setPdfDocument(PDDocument doc, PdfHighlighter highlighter) {
        this.pdDocument = doc;
        this.highlighter = highlighter;
    }

    public void setSearchQuery(String query, List<Integer> resultsPages) {
        this.searchQuery = query;
        this.searchResultsPages = resultsPages;
        notifyDataSetChanged();
    }

    public void setHighlightColors(int highlight, int active) {
        this.highlightColor = highlight;
        this.activeHighlightColor = active;
        notifyDataSetChanged();
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
        
        // Apply Global Scale
        ViewGroup.LayoutParams params = holder.imageView.getLayoutParams();
        if (params != null) {
            // Base width is usually match_parent, but we need concrete pixels for scaling.
            // We assume a standard width base (e.g. screen width) or just scale current measured width if available?
            // "safe" approach: assume 1080p base or layout based.
            // Better: Set width to MATCH_PARENT in xml, and here set width to:
            // DisplayMetrics metrics = holder.itemView.getContext().getResources().getDisplayMetrics();
            // int baseWidth = metrics.widthPixels;
            // params.width = (int) (baseWidth * globalScale);
            // params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // image view adjusts height via adjustViewBounds
            
            int baseWidth = holder.itemView.getContext().getResources().getDisplayMetrics().widthPixels;
            // Add padding consideration if needed
            
            params.width = (int) (baseWidth * globalScale);
            if (params.width < baseWidth) params.width = baseWidth; // unexpected
            
            holder.imageView.setLayoutParams(params);
        }

        // Set on ZoomLayout (now FrameLayout/View) since it consumes touches but we removed the custom view logic
        if (holder.zoomLayout != null) {
            holder.zoomLayout.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onPageLongClick(holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
        }
        
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
            
            // Draw Highlights
            if (pdDocument != null && highlighter != null && searchQuery != null && !searchQuery.isEmpty()) {
                 // Check if this page has matches (optimization)
                 boolean hasMatch = true; 
                 if (searchResultsPages != null && !searchResultsPages.contains(index)) {
                     hasMatch = false;
                 }
                 
                 if (hasMatch) {
                     try {
                         List<RectF> rects;
                         synchronized(pdDocument) {
                             rects = highlighter.searchPage(pdDocument, index, searchQuery);
                         }
                         
                         if (!rects.isEmpty()) {
                             Canvas canvas = new Canvas(bitmap);
                             Paint paint = new Paint();
                             paint.setColor(highlightColor);
                             paint.setStyle(Paint.Style.FILL);
                             
                             for (RectF rect : rects) {
                                 // Rect is in PDF coordinates (user space points)
                                 // We need to scale to Bitmap pixels.
                                 // PdfRenderer scale: `scale` variable.
                                 
                                 // PDFBox coordinate system assumption vs Android PdfRenderer:
                                 // PdfRenderer works on "Points" (1/72 inch). 
                                 // PDFBox works on "Points".
                                 // However, local coordinate origin might differ.
                                 // PdfRenderer: (0,0) is Top-Left.
                                 // PDFBox: (0,0) is usually Bottom-Left for content stream, but converted to Top-Left in some contexts?
                                 // My PdfHighlighter tries to produce Top-Left Y.
                                 
                                 // Map rect to bitmap
                                 RectF mappedRect = new RectF(
                                     rect.left * scale,
                                     rect.top * scale,
                                     rect.right * scale,
                                     rect.bottom * scale
                                 );
                                 
                                 canvas.drawRect(mappedRect, paint);
                             }
                         }
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                 }
            }

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
        android.view.View zoomLayout; // Changed from ZoomLayout to View (FrameLayout)

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pdfPageImage);
            zoomLayout = itemView.findViewById(R.id.zoomLayout);
            // No specialized calls needed for FrameLayout
            pageNumber = itemView.findViewById(R.id.pdfPageNumber);
        }
    }
}
