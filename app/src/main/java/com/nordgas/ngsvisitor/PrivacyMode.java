package com.nordgas.ngsvisitor;

import android.app.Activity;
import android.os.Build;
import android.view.WindowManager;

public final class PrivacyMode {
    private PrivacyMode() {}

    public static void apply(Activity activity) {
        boolean enabled = new SecurityPreferences(activity).privacyEnabled();
        if (enabled) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            activity.setRecentsScreenshotEnabled(!enabled);
        }
    }
}
