package com.dinanathdash.officeapp;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dinanathdash.officeapp.ui.TextBottomSheetFragment;
import com.dinanathdash.officeapp.utils.UpdateManager;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        
        // Window Insets


        // Initialize Views
        View btnBack = findViewById(R.id.btnBack);
        TextView tvVersion = findViewById(R.id.tvVersion);
        
        View btnUpdate = findViewById(R.id.btnUpdate);
        View btnPrivacy = findViewById(R.id.btnPrivacy);
        View btnLicense = findViewById(R.id.btnLicense);

        // Set Version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Listeners
        btnBack.setOnClickListener(v -> finish());
        
        btnUpdate.setOnClickListener(v -> {
            UpdateManager updateManager = new UpdateManager(this);
            updateManager.checkForUpdates();
        });

        btnPrivacy.setOnClickListener(v -> {
            TextBottomSheetFragment.newInstance(getString(R.string.action_privacy), getString(R.string.privacy_policy_text))
                    .show(getSupportFragmentManager(), "PrivacyDialog");
        });

        btnLicense.setOnClickListener(v -> {
             TextBottomSheetFragment.newInstance(getString(R.string.action_license), getString(R.string.license_text) + "<br/><br/>" + getString(R.string.open_source_licenses_text))
                    .show(getSupportFragmentManager(), "LicenseDialog");
        });
    }
}
