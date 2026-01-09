package com.dinanathdash.officeapp.utils;

import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BottomNavHelper {
    /**
     * Sets up the bottom navigation spacer to match the system navigation bar height.
     * Call this in onCreate after setContentView.
     * 
     * @param spacer The bottom spacer view (should have id bottomNavSpacer)
     */
    public static void setupBottomSpacer(View spacer) {
        if (spacer == null) return;
        
        ViewCompat.setOnApplyWindowInsetsListener(spacer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Set the spacer height to match the bottom inset
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = systemBars.bottom;
            v.setLayoutParams(params);
            
            return insets;
        });
    }
}
