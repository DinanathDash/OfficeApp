package com.dinanathdash.officeapp.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dinanathdash.officeapp.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class UpdateBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_VERSION = "arg_version";
    private static final String ARG_URL = "arg_url";
    private static final String ARG_NOTES = "arg_notes";

    public static UpdateBottomSheetFragment newInstance(String version, String url, String notes) {
        UpdateBottomSheetFragment fragment = new UpdateBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VERSION, version);
        args.putString(ARG_URL, url);
        args.putString(ARG_NOTES, notes);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_update_bottom_sheet, container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set background to our custom drawable so it extends behind the nav bar
                bottomSheet.setBackgroundResource(R.drawable.rounded_bottom_sheet_bg);
                
                // Set background of the content view to our drawable (optional depending on how layout is inflated, but good practice per reference)
                // Actually the layout root has the background, so we just need transparent container
                
                // Expand
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

        String version = getArguments().getString(ARG_VERSION);
        String url = getArguments().getString(ARG_URL);
        String notes = getArguments().getString(ARG_NOTES);

        TextView tvVersionInfo = view.findViewById(R.id.tvVersionInfo);
        TextView tvReleaseNotes = view.findViewById(R.id.tvReleaseNotes);
        View btnUpdate = view.findViewById(R.id.btnUpdate);
        View btnLater = view.findViewById(R.id.btnLater);

        tvVersionInfo.setText("A new version " + version + " is available.");
        tvReleaseNotes.setText(notes);

        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            dismiss();
        });

        btnLater.setOnClickListener(v -> dismiss());
    }
}
