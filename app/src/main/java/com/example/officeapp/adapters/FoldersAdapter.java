package com.example.officeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officeapp.R;
import com.example.officeapp.data.Folder;

import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.ViewHolder> {

    private List<Folder> folders;

    public FoldersAdapter(List<Folder> folders) {
        this.folders = folders;
    }

    public void updateList(List<Folder> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.tvFolderName.setText(folder.getName());
        holder.tvFileCount.setText(folder.getFileCount() + " files");
        holder.tvFolderSize.setText(folder.getSize());
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName;
        TextView tvFileCount;
        TextView tvFolderSize;

        ViewHolder(View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            tvFileCount = itemView.findViewById(R.id.tvFileCount);
            tvFolderSize = itemView.findViewById(R.id.tvFolderSize);
        }
    }
}
