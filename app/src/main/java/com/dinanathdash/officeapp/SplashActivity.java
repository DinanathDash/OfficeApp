package com.dinanathdash.officeapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_IS_FIRST_RUN = "is_first_run";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        boolean isFirstRun = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_IS_FIRST_RUN, true);

        View btnGetStarted = findViewById(R.id.btnGetStarted);

        if (isFirstRun) {
            // First Run: Show button and wait for user interaction
            if (btnGetStarted != null) {
                btnGetStarted.setVisibility(View.VISIBLE);
                btnGetStarted.setOnClickListener(v -> {
                    // Save first run complete
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_IS_FIRST_RUN, false)
                            .apply();
                    
                    navigateToMain();
                });
            }
        } else {
            // Subsequent Runs: specific logic if needed, or just redirect
             if (btnGetStarted != null) {
                btnGetStarted.setVisibility(View.GONE); // Hide button if shown by default
            }
            
            // Auto-redirect
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, 2000);
        }
        
        // Optional Animations for Title/Subtitle could go here if needed
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
