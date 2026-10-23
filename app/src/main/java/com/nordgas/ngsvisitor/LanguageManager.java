package com.nordgas.ngsvisitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class LanguageManager {
    private static final String PREFS = "ngs_ui_language";
    private static final String KEY = "language";
    private static final String CHINESE = "zh";
    private static final String ENGLISH = "en";

    private LanguageManager() {}

    public static Context wrap(Context base) {
        String language = current(base);
        Locale locale = ENGLISH.equals(language) ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return base.createConfigurationContext(configuration);
    }

    public static String current(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return preferences.getString(KEY, CHINESE);
    }

    public static void toggle(Context context) {
        String next = ENGLISH.equals(current(context)) ? CHINESE : ENGLISH;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, next).apply();
    }

    public static String switchButtonLabel(Context context) {
        return ENGLISH.equals(current(context)) ? "中文" : "EN";
    }
}
