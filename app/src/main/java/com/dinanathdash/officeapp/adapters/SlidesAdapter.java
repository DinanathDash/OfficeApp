package com.dinanathdash.officeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.R;
import com.dinanathdash.officeapp.data.Slide;

import java.util.List;

public class SlidesAdapter extends RecyclerView.Adapter<SlidesAdapter.ViewHolder> {

    private List<Slide> slides;
    private String searchQuery = "";

    public SlidesAdapter(List<Slide> slides) {
        this.slides = slides;
    }

    public void updateList(List<Slide> newSlides) {
        this.slides = newSlides;
        notifyDataSetChanged();
    }
    
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false);
        return new ViewHolder(view);
    }

    private int activeSlidePos = -1;
    private int activeFieldType = -1; // 0=Title, 1=Content, 2=Notes
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
        
        if (slide.getTitle() == null || slide.getTitle().isEmpty()) {
            holder.tvTitle.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setVisibility(View.VISIBLE);
            setHighlightedText(holder.tvTitle, slide.getTitle(), position, 0);
        }
        
        setHighlightedText(holder.tvContent, slide.getContent(), position, 1);
        
        if (slide.getNotes() == null || slide.getNotes().trim().isEmpty()) {
            holder.layoutNotes.setVisibility(View.GONE);
        } else {
            holder.layoutNotes.setVisibility(View.VISIBLE);
            setHighlightedText(holder.tvNotes, slide.getNotes(), position, 2);
        }
    }
    
    private void setHighlightedText(TextView textView, String text, int slidePos, int fieldType) {
        if (text == null) text = "";
        
        if (searchQuery.isEmpty()) {
            textView.setText(text);
            return;
        }

        android.text.SpannableString spannableString = new android.text.SpannableString(text);
        String lowerCaseText = text.toLowerCase();
        String lowerCaseQuery = searchQuery.toLowerCase();
        int index = lowerCaseText.indexOf(lowerCaseQuery);
        
        while (index >= 0) {
            boolean isActive = (slidePos == activeSlidePos && fieldType == activeFieldType && index == activeChaIndex);
            int colorRes = isActive ? R.color.search_highlight_active : R.color.search_highlight;
            
            spannableString.setSpan(new android.text.style.BackgroundColorSpan(androidx.core.content.ContextCompat.getColor(textView.getContext(), colorRes)), 
                    index, index + searchQuery.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = lowerCaseText.indexOf(lowerCaseQuery, index + searchQuery.length());
        }
        textView.setText(spannableString);
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlideNumber, tvTitle, tvContent, tvNotes;
        LinearLayout layoutNotes;

        ViewHolder(View itemView) {
            super(itemView);
            tvSlideNumber = itemView.findViewById(R.id.tvSlideNumber);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            layoutNotes = itemView.findViewById(R.id.layoutNotes);
            com.dinanathdash.officeapp.ui.ZoomLayout zoomLayout = itemView.findViewById(R.id.zoomLayout);
            if (zoomLayout != null) {
                zoomLayout.setScrollableAtScaleOne(false); // Let RecyclerView handle scrolling when not zoomed
                zoomLayout.setMeasureMode(com.dinanathdash.officeapp.ui.ZoomLayout.MeasureMode.UNBOUNDED_BOTH); // Allow content to be full size
            }
        }
    }
}
