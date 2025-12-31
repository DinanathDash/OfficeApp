package com.dinanathdash.officeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.R;
import com.dinanathdash.officeapp.data.Slide;
import com.dinanathdash.officeapp.ui.SlideView;

import java.util.List;

import com.dinanathdash.officeapp.ui.GlobalZoomHelper;

public class SlidesAdapter extends RecyclerView.Adapter<SlidesAdapter.ViewHolder> implements GlobalZoomHelper.ZoomableAdapter {

    private List<Slide> slides;
    private String searchQuery = "";
    private int highlightColor = 0;
    private int activeHighlightColor = 0;
    
    private float globalScale = 1.0f;

    @Override
    public void setGlobalScale(float scale) {
        this.globalScale = scale;
        notifyDataSetChanged();
    }

    public SlidesAdapter(List<Slide> slides) {
        this.slides = slides;
    }

    public void updateList(List<Slide> newSlides) {
        this.slides = newSlides;
        notifyDataSetChanged();
    }
    
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        // Search inside SlideView not fully implemented visually yet, but we keep the robust query field
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false);
        return new ViewHolder(view);
    }

    private int activeSlidePos = -1;
    private int activeFieldType = -1; 
    private int activeChaIndex = -1;

    public void setActiveMatch(int slidePos, int fieldType, int charIndex) {
        this.activeSlidePos = slidePos;
        this.activeFieldType = fieldType;
        this.activeChaIndex = charIndex;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Slide slide = slides.get(position);
        
        holder.tvSlideNumber.setText("Slide " + slide.getSlideNumber());
        
        // Apply global scale
        ViewGroup.LayoutParams params = holder.slideView.getLayoutParams();
        if (params != null) {
            int baseWidth = holder.itemView.getContext().getResources().getDisplayMetrics().widthPixels;
            // Subtract margins if any (CardView margins = 16dp total?)
            // Ideally we measure available width, but baseWidth is a good enough proxy for fullscreen
            int targetWidth = (int) ((baseWidth - 40) * globalScale); // 40px approximate padding/margin
            if (targetWidth < baseWidth - 40) targetWidth = baseWidth - 40;
            
            params.width = targetWidth;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // SlideView calculates height based on width
            holder.slideView.setLayoutParams(params);
        }

        holder.slideView.setSlide(slide);
        holder.slideView.setSearchQuery(searchQuery);
        if (highlightColor != 0) {
            holder.slideView.setHighlightColors(highlightColor, activeHighlightColor);
        }
        
        holder.slideView.setOnContentLongClickListener((text) -> {
            if (holder.itemView.getContext() instanceof com.dinanathdash.officeapp.PptActivity) {
                ((com.dinanathdash.officeapp.PptActivity) holder.itemView.getContext())
                    .showTextDialog("Slide " + slide.getSlideNumber() + " Text", text);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlideNumber;
        SlideView slideView;

        ViewHolder(View itemView) {
            super(itemView);
            tvSlideNumber = itemView.findViewById(R.id.tvSlideNumber);
            slideView = itemView.findViewById(R.id.slideView);
        }
    }
}
