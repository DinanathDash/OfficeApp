package com.example.officeapp.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.officeapp.R;
import com.example.officeapp.utils.FileUtils;
import com.example.officeapp.data.RecentFile;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class FileDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_FILE_NAME = "arg_file_name";
    private static final String ARG_FILE_URI = "arg_file_uri";
    private static final String ARG_FILE_TIMESTAMP = "arg_file_timestamp";

    public static FileDetailsBottomSheetFragment newInstance(RecentFile file) {
        FileDetailsBottomSheetFragment fragment = new FileDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, file.getName());
        args.putString(ARG_FILE_URI, file.getUriString());
        args.putLong(ARG_FILE_TIMESTAMP, file.getTimestamp());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_file_details, container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set background to transparent so our custom drawable with rounded corners shows
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                
                // Set background of the content view to our drawable
                View contentView = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (contentView != null) {
                    contentView.setBackgroundResource(R.drawable.rounded_bottom_sheet_bg);
                }
                
                // Expand fully
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) return;

        String fileName = getArguments().getString(ARG_FILE_NAME);
        String uriString = getArguments().getString(ARG_FILE_URI);
        long timestamp = getArguments().getLong(ARG_FILE_TIMESTAMP);

        // Header
        view.findViewById(R.id.btnBack).setOnClickListener(v -> dismiss());

        // File Details
        TextView tvFileName = view.findViewById(R.id.tvFileName);
        TextView tvSize = view.findViewById(R.id.tvSize);
        TextView tvFormat = view.findViewById(R.id.tvFormat);
        TextView tvLocation = view.findViewById(R.id.tvLocation);
        TextView tvLastModified = view.findViewById(R.id.tvLastModified);
        android.widget.ImageView ivFileIcon = view.findViewById(R.id.ivFileIcon);

        tvFileName.setText(fileName);

        // Process URI for details
        try {
            Uri uri = Uri.parse(uriString);
            
            // Format
            String ext = FileUtils.getFileExtension(requireContext(), uri);
            tvFormat.setText(ext != null ? ext.toUpperCase() : "UNKNOWN");
            
            // Set Icon based on format
            ivFileIcon.setImageResource(FileUtils.getFileIconResource(ext));

            // Size
            long sizeBytes = FileUtils.getFileSize(requireContext(), uri);
            tvSize.setText(formatSize(sizeBytes));

            // Location (Decode path from URI if possible or just show URI)
            String path = uri.getPath();
            if (path != null && path.contains(":")) {
                // Try to make it look a bit cleaner if it is a content URI
                 path = path.substring(path.lastIndexOf(":") + 1);
            }
            // For now show the user string, normally we'd resolve real path
            tvLocation.setText("Location: " + uriString); 

            // Date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault());
            tvLastModified.setText(sdf.format(new java.util.Date(timestamp)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 KB";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
