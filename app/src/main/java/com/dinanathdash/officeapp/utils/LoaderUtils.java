package com.dinanathdash.officeapp.utils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.LottieValueCallback;

public class LoaderUtils {
    public static void applyThemeColors(LottieAnimationView animationView, int color) {
        if (animationView == null) return;
        
        // Apply color to Strokes only to preserve the outlined look (white fills remain white)
        animationView.addValueCallback(
            new KeyPath("**", "Stroke 1"),
            LottieProperty.STROKE_COLOR,
            new LottieValueCallback<>(color)
        );
    }
}
