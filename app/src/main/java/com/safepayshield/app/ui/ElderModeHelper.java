package com.safepayshield.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.button.MaterialButton;
import com.safepayshield.app.R;

public final class ElderModeHelper {
    private ElderModeHelper() {
    }

    public static void apply(Context context, View root) {
        if (context == null || root == null) {
            return;
        }
        SharedPreferences prefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            prefs = EncryptedSharedPreferences.create(
                context,
                "safepay_shield_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception error) {
            prefs = context.getSharedPreferences("safepay_shield_fallback", Context.MODE_PRIVATE);
        }
        boolean elderMode = prefs.getBoolean("elder_mode", false);
        applyInternal(root, elderMode);
    }

    private static void applyInternal(View view, boolean elderMode) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            float base = tv.getTextSize();
            if (elderMode) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * 1.12f);
                tv.setLineSpacing(4f, 1.1f);
                if (tv.getPaddingLeft() == 0 && tv.getPaddingTop() == 0 && tv.getPaddingRight() == 0 && tv.getPaddingBottom() == 0) {
                    tv.setPadding(8, 8, 8, 8);
                }
            }
        }

        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            if (elderMode) {
                int minHeight = (int) (button.getMinHeight() > 0 ? button.getMinHeight() * 1.1f : dp(button.getContext(), 52));
                button.setMinHeight(minHeight);
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
            }
        }

        if (view.getBackground() == null && elderMode) {
            view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.white));
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyInternal(group.getChildAt(i), elderMode);
            }
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
