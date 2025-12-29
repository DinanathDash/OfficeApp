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

    private boolean isGridMode = false;

    public RecentFilesAdapter(List<RecentFile> files, OnItemClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    public void setGridMode(boolean isGridMode) {
        this.isGridMode = isGridMode;
        notifyDataSetChanged();
    }
    
    // Filter logic
    public void filterList(List<RecentFile> filteredFiles) {
        this.files = filteredFiles;
        notifyDataSetChanged();
    }

    public void updateList(List<RecentFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridMode ? R.layout.item_recent_file_grid : R.layout.item_recent_file;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentFile file = files.get(position);
        holder.fileName.setText(file.getName());
        
        // Simple date formatting
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy h:mm a", java.util.Locale.getDefault());
        String dateStr = sdf.format(new java.util.Date(file.getTimestamp()));
        holder.fileDate.setText(dateStr);

        // Set Icon
        String ext = com.example.officeapp.utils.FileUtils.getFileExtension(holder.itemView.getContext(), android.net.Uri.parse(file.getUriString()));
        holder.ivFileIcon.setImageResource(com.example.officeapp.utils.FileUtils.getFileIconResource(ext));
        holder.ivFileIcon.setImageTintList(null); // Remove any tint if present in XML layout
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(file));
        
        holder.ivMore.setOnClickListener(v -> {
            // Inflate custom layout
            View popupView = LayoutInflater.from(v.getContext()).inflate(R.layout.popup_file_options, null);
            
            // Create PopupWindow
            final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                    popupView, 
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    true);
            
            // Set background to the rounded drawable to get correct shadow shape
            popupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(v.getContext(), R.drawable.popup_bg));
            popupWindow.setElevation(16); // Standard elevation

            
            // Find views
            View btnDetails = popupView.findViewById(R.id.action_details);
            View btnShare = popupView.findViewById(R.id.action_share);
            View btnDelete = popupView.findViewById(R.id.action_delete);

            // Listeners
            btnDetails.setOnClickListener(view -> {
                if (listener instanceof OnFileActionListener) {
                    ((OnFileActionListener) listener).onDetailsFile(file);
                }
                popupWindow.dismiss();
            });

            btnShare.setOnClickListener(view -> {
                if (listener instanceof OnFileActionListener) {
                    ((OnFileActionListener) listener).onShareFile(file);
                }
                popupWindow.dismiss();
            });

            btnDelete.setOnClickListener(view -> {
                if (listener instanceof OnFileActionListener) {
                    ((OnFileActionListener) listener).onDeleteFile(file);
                }
                popupWindow.dismiss();
            });

            // Show popup
            // Use showAsDropDown to anchor it. 
            // The user requested "popup shouldn't be to the end of the screen".
            // Default showAsDropDown aligns left edge of popup with left edge of anchor.
            // We want it likely to the left of the anchor (dots) so it doesn't get cut off or stick to the extreme right.
            // But usually the dots are on the Right. So we want the popup to align its Right edge with the anchor's Right edge.
            
            // Calculate width first
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = popupView.getMeasuredWidth();

            // Calculate x-offset to align right edge of popup with right edge of icon
            // We want it slightly shifted left so it doesn't touch the screen edge
            float density = v.getResources().getDisplayMetrics().density;
            int offsetDp = -16; // Shift left by 16dp extra for safety
            int xoff = (v.getWidth() - width) + (int)(offsetDp * density);
            
            // If width measurement is 0 (sometimes happens with UNSPECIFIED), fallback to a reasonable estimate
            if (width <= 0) {
                 xoff = -((int)(180 * density)); // Fallback offset
            }

            popupWindow.showAsDropDown(v, xoff, -20);
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }
    
    // Interface update

    
    public interface OnFileActionListener extends OnItemClickListener {
        void onShareFile(RecentFile file);
        void onDeleteFile(RecentFile file);
        void onDetailsFile(RecentFile file);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        TextView fileDate;
        android.widget.ImageView ivMore;
        android.widget.ImageView ivFileIcon;

        ViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileDate = itemView.findViewById(R.id.fileDate);
            ivMore = itemView.findViewById(R.id.ivMore);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }
    }
}
