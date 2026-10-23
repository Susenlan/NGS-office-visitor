package com.nordgas.ngsvisitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public final class AdminLoginActivity extends Activity {
    public static final String EXTRA_CHANGE = "change_credentials";
    private static final int ID_USERNAME = 2001;
    private static final int ID_PASSWORD = 2002;
    private static final int ID_CONFIRM = 2003;

    private AdminCredentialStore credentials;
    private EditText username;
    private EditText password;
    private EditText confirm;
    private boolean creating;
    private boolean changing;

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrivacyMode.apply(this);
        credentials = new AdminCredentialStore(this);
        changing = getIntent().getBooleanExtra(EXTRA_CHANGE, false);
        creating = !credentials.hasCredentials();
        if (changing && !AdminSession.isUnlocked()) {
            finish();
            return;
        }
        setContentView(buildUi());
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

        TextView title = text(getString(titleResource()), 21, getColor(R.color.navy));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.leftMargin = dp(14);
        brandBar.addView(title, titleParams);

        Button language = secondaryButton(LanguageManager.switchButtonLabel(this));
        language.setContentDescription(getString(R.string.switch_language));
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
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(24), dp(24), dp(32));

        TextView help = text(getString(creating
                        ? R.string.admin_first_use_help : R.string.admin_login_help),
                14, getColor(R.color.text_secondary));
        help.setPadding(0, 0, 0, dp(18));
        form.addView(help);

        username = addField(form, ID_USERNAME, R.string.admin_username,
                R.string.admin_username_hint, false);
        password = addField(form, ID_PASSWORD, R.string.admin_password,
                R.string.admin_password_hint, true);
        if (creating || changing) {
            confirm = addField(form, ID_CONFIRM, R.string.admin_confirm_password,
                    R.string.admin_password_hint, true);
        }

        Button submit = new Button(this);
        submit.setText(submitResource());
        submit.setTextColor(getColor(R.color.white));
        submit.setTextSize(16);
        submit.setAllCaps(false);
        submit.setBackgroundResource(R.drawable.brand_button_primary);
        submit.setOnClickListener(v -> submit());
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        submitParams.topMargin = dp(22);
        form.addView(submit, submitParams);

        scroll.addView(form);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        SafeArea.apply(this, brandBar, scroll);
        return screen;
    }

    private void submit() {
        String account = username.getText().toString().trim();
        char[] secret = password.getText().toString().toCharArray();
        try {
            if (creating || changing) {
                if (account.isEmpty()) {
                    toast(R.string.admin_username_required);
                    return;
                }
                if (secret.length < 10) {
                    toast(R.string.admin_password_policy);
                    return;
                }
                char[] confirmation = confirm.getText().toString().toCharArray();
                try {
                    if (!Arrays.equals(secret, confirmation)) {
                        toast(R.string.admin_password_mismatch);
                        return;
                    }
                } finally {
                    Arrays.fill(confirmation, '\0');
                }
                credentials.createOrReplace(account, secret);
                AdminSession.unlock();
                toast(changing ? R.string.admin_credentials_changed : R.string.admin_created);
                if (changing) {
                    finish();
                } else {
                    openSettings();
                }
                return;
            }

            long remaining = credentials.remainingLockMillis();
            if (remaining > 0) {
                Toast.makeText(this, getString(R.string.admin_locked,
                        Math.max(1L, (remaining + 999L) / 1000L)), Toast.LENGTH_LONG).show();
                return;
            }
            if (credentials.verify(account, secret)) {
                credentials.clearFailures();
                AdminSession.unlock();
                openSettings();
            } else {
                credentials.recordFailure();
                remaining = credentials.remainingLockMillis();
                if (remaining > 0) {
                    Toast.makeText(this, getString(R.string.admin_locked,
                            Math.max(1L, (remaining + 999L) / 1000L)), Toast.LENGTH_LONG).show();
                } else {
                    toast(R.string.admin_invalid);
                }
            }
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
    }

    private EditText addField(LinearLayout form, int id, int labelRes,
                              int hintRes, boolean passwordField) {
        TextView label = text(getString(labelRes), 13, getColor(R.color.text_secondary));
        label.setPadding(dp(4), dp(10), dp(4), dp(5));
        form.addView(label);

        EditText field = new EditText(this);
        field.setId(id);
        field.setHint(hintRes);
        field.setSingleLine(true);
        field.setTextSize(16);
        field.setTextColor(getColor(R.color.navy));
        field.setHintTextColor(getColor(R.color.text_secondary));
        field.setBackgroundResource(R.drawable.brand_field);
        field.setContentDescription(getString(labelRes));
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (passwordField) {
            field.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        form.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(getColor(R.color.navy));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setBackgroundResource(R.drawable.brand_button);
        return button;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int titleResource() {
        if (changing) return R.string.admin_change_title;
        return creating ? R.string.admin_create_title : R.string.admin_login_title;
    }

    private int submitResource() {
        if (changing) return R.string.admin_update;
        return creating ? R.string.admin_create : R.string.admin_sign_in;
    }

    private void toast(int resource) {
        Toast.makeText(this, resource, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
