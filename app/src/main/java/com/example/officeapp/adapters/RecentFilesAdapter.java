package com.example.officeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officeapp.R;
import com.example.officeapp.data.RecentFile;

import java.util.List;

public class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {

    private List<RecentFile> files;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecentFile file);
    }

    public RecentFilesAdapter(List<RecentFile> files, OnItemClickListener listener) {
        this.files = files;
        this.listener = listener;
    }
    
    public void updateList(List<RecentFile> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentFile file = files.get(position);
        holder.fileName.setText(file.getName());
        holder.fileType.setText(file.getType());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(file));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        TextView fileType;

        ViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileType = itemView.findViewById(R.id.fileType);
        }
    }
}
