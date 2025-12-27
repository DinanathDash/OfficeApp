package com.example.officeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officeapp.R;
import com.example.officeapp.data.Slide;

import java.util.List;

public class SlidesAdapter extends RecyclerView.Adapter<SlidesAdapter.ViewHolder> {

    private List<Slide> slides;

    public SlidesAdapter(List<Slide> slides) {
        this.slides = slides;
    }

    public void updateList(List<Slide> newSlides) {
        this.slides = newSlides;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Slide slide = slides.get(position);
        
        holder.tvSlideNumber.setText("Slide " + slide.getSlideNumber());
        
        if (slide.getTitle() == null || slide.getTitle().isEmpty()) {
            holder.tvTitle.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setVisibility(View.VISIBLE);
            holder.tvTitle.setText(slide.getTitle());
        }
        
        holder.tvContent.setText(slide.getContent());
        
        if (slide.getNotes() == null || slide.getNotes().trim().isEmpty()) {
            holder.layoutNotes.setVisibility(View.GONE);
        } else {
            holder.layoutNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(slide.getNotes());
        }
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
        }
    }
}
