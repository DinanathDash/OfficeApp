package com.dinanathdash.officeapp.ui;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.dinanathdash.officeapp.R;

public class DeveloperInfoFragment extends BottomSheetDialogFragment {

    public static DeveloperInfoFragment newInstance() {
        return new DeveloperInfoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_developer_info, container, false);
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
                
                // Set background of the content view to our drawable to extend behind nav bar
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

        View btnWebsite = view.findViewById(R.id.btnWebsite);
        View btnGitHub = view.findViewById(R.id.btnGitHub);
        View btnContact = view.findViewById(R.id.btnContact);

        btnWebsite.setOnClickListener(v -> openUrl(getString(R.string.dev_website_url)));
        btnGitHub.setOnClickListener(v -> openUrl(getString(R.string.dev_github_url)));
        
        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.dev_email)});
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.dev_email_subject));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
