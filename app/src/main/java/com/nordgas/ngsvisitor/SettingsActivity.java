package com.nordgas.ngsvisitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    private static final int ID_BASE_URL = 1001;
    private static final int ID_SITE_ID = 1002;
    private static final int ID_API_KEY = 1003;
    private static final int ID_GUEST_LIMIT = 1004;
    private static final int ID_FINGERPRINT = 1005;
    private EditText baseUrl;
    private EditText siteId;
    private EditText apiKey;
    private EditText fingerprint;
    private EditText guestLimit;
    private CheckBox privacyMode;
    private CheckBox kioskMode;
    private SecureConfigStore store;
    private boolean authorized;

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrivacyMode.apply(this);
        if (!AdminSession.isUnlocked()) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
            return;
        }
        authorized = true;
        store = new SecureConfigStore(this);
        setContentView(buildUi());
        loadValues();
    }

    @Override protected void onResume() {
        super.onResume();
        PrivacyMode.apply(this);
        if (authorized && !AdminSession.isUnlocked()) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        }
    }

    @Override protected void onDestroy() {
        if (isFinishing()) AdminSession.clear();
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(getColor(R.color.surface));

        LinearLayout brandBar = new LinearLayout(this);
        brandBar.setGravity(Gravity.CENTER_VERTICAL);
        brandBar.setPadding(dp(16), dp(10), dp(12), dp(10));
        brandBar.setBackgroundColor(getColor(R.color.white));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ngs_logo_vertical);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setContentDescription(getString(R.string.app_name));
        brandBar.addView(logo, new LinearLayout.LayoutParams(dp(92), dp(48)));

        TextView title = text(getString(R.string.settings_title), 21, getColor(R.color.navy));
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.leftMargin = dp(14);
        brandBar.addView(title, titleParams);

        Button language = new Button(this);
        language.setText(LanguageManager.switchButtonLabel(this));
        language.setTextColor(getColor(R.color.navy));
        language.setTextSize(14);
        language.setBackgroundResource(R.drawable.brand_button);
        language.setContentDescription(getString(R.string.switch_language));
        language.setAllCaps(false);
        language.setMinWidth(0);
        language.setOnClickListener(v -> {
            LanguageManager.toggle(this);
            recreate();
        });
        brandBar.addView(language, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        screen.addView(brandBar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(28));
        root.setBackgroundColor(getColor(R.color.surface));

        TextView intro = text(getString(R.string.settings_intro), 14,
                getColor(R.color.text_secondary));
        intro.setPadding(0, 0, 0, dp(18));
        root.addView(intro);

        TextView securityTitle = text(getString(R.string.security_section), 18,
                getColor(R.color.navy));
        securityTitle.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        securityTitle.setPadding(0, 0, 0, dp(6));
        root.addView(securityTitle);

        privacyMode = new CheckBox(this);
        privacyMode.setText(R.string.privacy_mode);
        privacyMode.setTextColor(getColor(R.color.navy));
        privacyMode.setTextSize(15);
        root.addView(privacyMode);
        root.addView(help(getString(R.string.privacy_help)));

        kioskMode = new CheckBox(this);
        kioskMode.setText(R.string.kiosk_mode);
        kioskMode.setTextColor(getColor(R.color.navy));
        kioskMode.setTextSize(15);
        root.addView(kioskMode);
        root.addView(help(getString(R.string.kiosk_help)));

        Button changeAdmin = new Button(this);
        changeAdmin.setText(R.string.change_admin_credentials);
        changeAdmin.setTextColor(getColor(R.color.navy));
        changeAdmin.setAllCaps(false);
        changeAdmin.setBackgroundResource(R.drawable.brand_button);
        changeAdmin.setOnClickListener(v -> {
            AdminSession.unlock();
            startActivity(new Intent(this, AdminLoginActivity.class)
                    .putExtra(AdminLoginActivity.EXTRA_CHANGE, true));
        });
        LinearLayout.LayoutParams changeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        changeParams.bottomMargin = dp(14);
        root.addView(changeAdmin, changeParams);

        baseUrl = addField(root, ID_BASE_URL, getString(R.string.api_root_label),
                getString(R.string.api_root_hint));
        root.addView(help(getString(R.string.api_root_help)));

        siteId = addField(root, ID_SITE_ID, getString(R.string.site_id_label),
                getString(R.string.site_id_hint));
        apiKey = addField(root, ID_API_KEY, getString(R.string.api_key_label),
                getString(R.string.api_key_hint));
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        guestLimit = addField(root, ID_GUEST_LIMIT, getString(R.string.fallback_limit_label),
                getString(R.string.fallback_limit_hint));
        guestLimit.setInputType(InputType.TYPE_CLASS_NUMBER);

        fingerprint = addField(root, ID_FINGERPRINT, getString(R.string.certificate_label),
                getString(R.string.certificate_hint));
        root.addView(help(getString(R.string.certificate_help)));

        Button save = new Button(this);
        save.setText(R.string.save_return);
        save.setTextColor(getColor(R.color.white));
        save.setTextSize(16);
        save.setBackgroundResource(R.drawable.brand_button_primary);
        save.setAllCaps(false);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(18);
        root.addView(save, params);

        scroll.addView(root);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        SafeArea.apply(this, brandBar, scroll);
        return screen;
    }

    private void loadValues() {
        AppConfig config = store.load();
        baseUrl.setText(config.baseUrl);
        siteId.setText(config.siteId);
        guestLimit.setText(String.valueOf(config.fallbackGuestLimit));
        fingerprint.setText(config.certificateSha256);
        if (store.hasApiKey()) apiKey.setHint(R.string.api_key_saved_hint);
        SecurityPreferences security = new SecurityPreferences(this);
        privacyMode.setChecked(security.privacyEnabled());
        kioskMode.setChecked(security.kioskEnabled());
    }

    private void save() {
        String url = baseUrl.getText().toString().trim();
        if (!url.startsWith("https://")) {
            Toast.makeText(this, R.string.error_https, Toast.LENGTH_LONG).show();
            return;
        }
        String key = apiKey.getText().toString().trim();
        if (key.trim().isEmpty() && !store.hasApiKey()) {
            Toast.makeText(this, R.string.error_api_key, Toast.LENGTH_LONG).show();
            return;
        }
        int limit;
        try {
            limit = Integer.parseInt(guestLimit.getText().toString());
        } catch (Exception e) {
            limit = 5;
        }
        store.save(new AppConfig(url, siteId.getText().toString(), key,
                fingerprint.getText().toString(), limit), true);
        SecurityPreferences security = new SecurityPreferences(this);
        security.setPrivacyEnabled(privacyMode.isChecked());
        security.setKioskEnabled(kioskMode.isChecked());
        PrivacyMode.apply(this);
        if (!kioskMode.isChecked()) {
            try {
                stopLockTask();
            } catch (Exception ignored) {
                // The activity was not running in screen-pinning or lock-task mode.
            }
        }
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private EditText addField(LinearLayout root, int id, String label, String hint) {
        TextView caption = text(label, 13, getColor(R.color.text_secondary));
        caption.setPadding(dp(4), dp(10), dp(4), dp(5));
        root.addView(caption);

        EditText edit = new EditText(this);
        edit.setId(id);
        edit.setHint(hint);
        edit.setSingleLine(true);
        edit.setTextSize(16);
        edit.setTextColor(getColor(R.color.navy));
        edit.setHintTextColor(getColor(R.color.text_secondary));
        edit.setBackgroundResource(R.drawable.brand_field);
        edit.setContentDescription(label);
        edit.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(edit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return edit;
    }

    private TextView help(String value) {
        TextView view = text(value, 12, getColor(R.color.text_secondary));
        view.setPadding(dp(4), dp(5), dp(4), dp(10));
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
