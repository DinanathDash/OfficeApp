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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.dinanathdash.officeapp.R;

public class TextBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_TEXT = "arg_text";

    public static TextBottomSheetFragment newInstance(String title, String text) {
        TextBottomSheetFragment fragment = new TextBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
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

        String title = getArguments().getString(ARG_TITLE);
        String text = getArguments().getString(ARG_TEXT);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvText = view.findViewById(R.id.tvExtractedText);
        View btnClose = view.findViewById(R.id.btnClose);

        tvTitle.setText(title);
        // Support HTML content for licenses etc.
        // Replace newlines with <br> to preserve formatting in HTML mode
        if (text != null) {
            text = text.replace("\n", "<br>");
        }
        tvText.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_COMPACT));

        btnClose.setOnClickListener(v -> dismiss());
    }
}
