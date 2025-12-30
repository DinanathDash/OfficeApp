package com.dinanathdash.officeapp.ui;

import android.app.Dialog;
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

public class ExtractedTextBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_TEXT = "arg_text";
    private static final String ARG_PAGE_NUMBER = "arg_page_number";

    public static ExtractedTextBottomSheetFragment newInstance(String text, int pageNumber) {
        ExtractedTextBottomSheetFragment fragment = new ExtractedTextBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_extracted_text, container, false);
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

        String text = getArguments().getString(ARG_TEXT);
        int pageNumber = getArguments().getInt(ARG_PAGE_NUMBER);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvText = view.findViewById(R.id.tvExtractedText);
        View btnClose = view.findViewById(R.id.btnClose);

        tvTitle.setText("Page " + (pageNumber + 1) + " Text");
        tvText.setText(text);
        
        btnClose.setOnClickListener(v -> dismiss());
    }
}
