package com.nordgas.ngsvisitor;

import android.content.Context;
import android.content.SharedPreferences;

public final class SecurityPreferences {
    private static final String PREFS = "ngs_security_preferences";
    private final SharedPreferences prefs;

    public SecurityPreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean privacyEnabled() {
        return prefs.getBoolean("privacy_enabled", true);
    }

    public void setPrivacyEnabled(boolean enabled) {
        prefs.edit().putBoolean("privacy_enabled", enabled).apply();
    }

    public boolean kioskEnabled() {
        return prefs.getBoolean("kiosk_enabled", false);
    }

    public void setKioskEnabled(boolean enabled) {
        prefs.edit().putBoolean("kiosk_enabled", enabled).apply();
    }
}
