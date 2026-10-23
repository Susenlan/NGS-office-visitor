package com.nordgas.ngsvisitor;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLHandshakeException;

public final class MainActivity extends Activity {
    private static final String STATE_FILTER = "selected_filter";
    private enum Filter { AVAILABLE, ACTIVE, UNAVAILABLE }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Voucher> allVouchers = new ArrayList<>();
    private final EnumMap<Filter, Button> filterButtons = new EnumMap<>(Filter.class);
    private VoucherAdapter adapter;
    private TextView summary;
    private TextView syncStatus;
    private ProgressBar progress;
    private ReservationStore reservations;
    private Filter filter = Filter.AVAILABLE;
    private final Runnable autoRefresh = this::refresh;

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrivacyMode.apply(this);
        if (savedInstanceState != null) {
            try {
                filter = Filter.valueOf(savedInstanceState.getString(
                        STATE_FILTER, Filter.AVAILABLE.name()));
            } catch (IllegalArgumentException ignored) {
                filter = Filter.AVAILABLE;
            }
        }
        reservations = new ReservationStore(this);
        setContentView(buildUi());
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_FILTER, filter.name());
        super.onSaveInstanceState(outState);
    }

    @Override protected void onResume() {
        super.onResume();
        PrivacyMode.apply(this);
        ClipboardSecurity.clearExpiredVoucher(this);
        SecureConfigStore store = new SecureConfigStore(this);
        if (!store.load().isComplete()) {
            openAdminLogin();
        } else {
            startKioskIfEnabled();
            refresh();
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(autoRefresh);
        super.onPause();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.surface));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(12), dp(10));
        header.setBackgroundColor(getColor(R.color.white));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ngs_logo_horizontal);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setContentDescription(getString(R.string.app_name));
        header.addView(logo, new LinearLayout.LayoutParams(0, dp(34), 1));

        Button language = button(LanguageManager.switchButtonLabel(this));
        language.setContentDescription(getString(R.string.switch_language));
        language.setOnClickListener(v -> {
            LanguageManager.toggle(this);
            recreate();
        });
        LinearLayout.LayoutParams languageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        languageParams.rightMargin = dp(6);
        header.addView(language, languageParams);

        ImageButton refresh = iconButton(R.drawable.ic_refresh, R.string.refresh);
        refresh.setOnClickListener(v -> refresh());
        header.addView(refresh, iconParams());

        ImageButton settings = iconButton(R.drawable.ic_settings, R.string.settings);
        settings.setOnClickListener(v -> openAdminLogin());
        header.addView(settings, iconParams());
        root.addView(header);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        body.setBackgroundColor(getColor(R.color.surface));

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundResource(R.drawable.brand_card);

        summary = label(getString(R.string.loading_vouchers), 18, getColor(R.color.navy), true);
        summary.setPadding(dp(18), dp(14), dp(18), dp(6));
        sidebar.addView(summary);

        syncStatus = label("", 13, getColor(R.color.text_secondary), false);
        syncStatus.setPadding(dp(18), 0, dp(18), dp(8));
        sidebar.addView(syncStatus);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(landscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        filters.setPadding(dp(10), 0, dp(10), dp(6));
        addFilter(filters, getString(R.string.filter_available), Filter.AVAILABLE, landscape);
        addFilter(filters, getString(R.string.filter_active), Filter.ACTIVE, landscape);
        addFilter(filters, getString(R.string.filter_unavailable), Filter.UNAVAILABLE, landscape);
        sidebar.addView(filters);
        updateFilterStyles();

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setBackgroundColor(getColor(R.color.surface));
        list.setClipToPadding(false);
        adapter = new VoucherAdapter(this, () -> {
            updateSummary();
            applyFilter();
        });
        list.setAdapter(adapter);
        if (landscape) {
            ScrollView sidebarScroll = new ScrollView(this);
            sidebarScroll.setFillViewport(true);
            sidebarScroll.setClipToPadding(false);
            sidebarScroll.setVerticalScrollBarEnabled(true);
            sidebarScroll.setBackgroundResource(R.drawable.brand_card);
            sidebar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            sidebarScroll.addView(sidebar, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(dp(300),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            sidebarParams.setMargins(dp(12), dp(12), dp(6), dp(12));
            body.addView(sidebarScroll, sidebarParams);
            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            listParams.setMargins(dp(6), dp(6), dp(6), dp(6));
            body.addView(list, listParams);
        } else {
            LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sidebarParams.setMargins(dp(12), dp(12), dp(12), dp(6));
            body.addView(sidebar, sidebarParams);
            body.addView(list, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }
        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        SafeArea.apply(this, header, body);
        return root;
    }

    private void addFilter(LinearLayout row, String text, Filter target, boolean vertical) {
        Button button = button(text);
        if (!vertical) button.setTextSize(12);
        button.setOnClickListener(v -> {
            filter = target;
            updateFilterStyles();
            applyFilter();
        });
        filterButtons.put(target, button);
        LinearLayout.LayoutParams params = vertical
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(button, params);
    }

    private void updateFilterStyles() {
        for (Filter value : Filter.values()) {
            Button button = filterButtons.get(value);
            if (button == null) continue;
            boolean selected = value == filter;
            button.setBackgroundResource(selected
                    ? R.drawable.brand_filter_selected : R.drawable.brand_filter_unselected);
            button.setTextColor(getColor(selected ? R.color.white : R.color.navy));
        }
    }

    private void refresh() {
        handler.removeCallbacks(autoRefresh);
        AppConfig config = new SecureConfigStore(this).load();
        if (!config.isComplete()) return;
        progress.setVisibility(View.VISIBLE);
        syncStatus.setText(R.string.syncing);

        executor.execute(() -> {
            try {
                UniFiApiClient client = new UniFiApiClient(config);
                String siteId = config.siteId;
                if (siteId == null || siteId.trim().isEmpty()) {
                    siteId = client.discoverSiteId();
                    new SecureConfigStore(this).saveSiteId(siteId);
                }
                List<Voucher> loaded = client.loadVouchers(siteId);
                loaded.sort(Comparator
                        .comparing((Voucher v) -> v.state(Instant.now()).ordinal())
                        .thenComparing(v -> v.expiresAt == null ? Instant.MAX : v.expiresAt)
                        .thenComparing(v -> v.code));
                runOnUiThread(() -> showData(loaded));
            } catch (Exception e) {
                runOnUiThread(() -> showError(e));
            }
        });
    }

    private void showData(List<Voucher> loaded) {
        for (Voucher voucher : loaded) {
            if (voucher.guestCount > 0 || !voucher.isAvailable(Instant.now())) {
                reservations.clear(voucher.code);
            }
        }
        allVouchers.clear();
        allVouchers.addAll(loaded);
        progress.setVisibility(View.GONE);
        syncStatus.setText(R.string.synced_auto);
        updateSummary();
        applyFilter();
        handler.postDelayed(autoRefresh, 30_000);
    }

    private void showError(Exception error) {
        progress.setVisibility(View.GONE);
        syncStatus.setText(getString(R.string.sync_failed, friendlyMessage(error)));
        Toast.makeText(this, R.string.cannot_read_unifi, Toast.LENGTH_LONG).show();
        handler.postDelayed(autoRefresh, 60_000);
    }

    private String friendlyMessage(Exception error) {
        if (containsMessage(error, "CERTIFICATE_MISMATCH")) {
            return getString(R.string.error_certificate_mismatch);
        }
        if (hasCause(error, SocketTimeoutException.class)) {
            return getString(R.string.network_timeout_sanitized);
        }
        if (hasCause(error, UnknownHostException.class)) {
            return getString(R.string.network_host_sanitized);
        }
        if (hasCause(error, ConnectException.class)
                || hasCause(error, NoRouteToHostException.class)) {
            return getString(R.string.network_unreachable_sanitized);
        }
        if (hasCause(error, SSLHandshakeException.class)) {
            return getString(R.string.certificate_untrusted);
        }
        if (containsMessage(error, "NO_SITES")) {
            return getString(R.string.error_no_sites);
        }
        if (containsMessage(error, "HTTPS_REQUIRED")) {
            return getString(R.string.error_https_runtime);
        }
        String httpStatus = messageValue(error, "HTTP_STATUS:");
        if (httpStatus != null) {
            if ("401".equals(httpStatus) || "403".equals(httpStatus)) {
                return getString(R.string.network_auth_sanitized);
            }
            if ("404".equals(httpStatus)) {
                return getString(R.string.network_endpoint_sanitized);
            }
            return getString(R.string.network_generic_sanitized);
        }
        if (hasCause(error, java.security.GeneralSecurityException.class)) {
            return getString(R.string.network_tls_sanitized);
        }
        return getString(R.string.network_generic_sanitized);
    }

    private boolean hasCause(Throwable error, Class<?> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private boolean containsMessage(Throwable error, String value) {
        return messageValue(error, value) != null;
    }

    private String messageValue(Throwable error, String prefix) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) continue;
            int index = message.indexOf(prefix);
            if (index >= 0) return message.substring(index + prefix.length()).trim();
        }
        return null;
    }

    private void openAdminLogin() {
        startActivity(new Intent(this, AdminLoginActivity.class));
    }

    private void startKioskIfEnabled() {
        if (!new SecurityPreferences(this).kioskEnabled()) return;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null
                && manager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE) return;
        try {
            startLockTask();
        } catch (SecurityException ignored) {
            // A managed device must allowlist this package for full lock-task mode.
        }
    }

    private void updateSummary() {
        Instant now = Instant.now();
        int usableCodes = 0;
        int slots = 0;
        int active = 0;
        int unavailable = 0;
        int waiting = 0;
        for (Voucher voucher : allVouchers) {
            boolean reserved = reservations.isReserved(voucher.code, now) && voucher.guestCount == 0;
            if (voucher.isAvailable(now) && !reserved) {
                usableCodes++;
                slots += voucher.remainingSlots();
                if (voucher.guestCount > 0) active++;
            } else if (reserved) {
                waiting++;
            } else {
                unavailable++;
            }
        }
        summary.setText(getString(R.string.summary,
                usableCodes, slots, waiting, active, unavailable));
    }

    private void applyFilter() {
        Instant now = Instant.now();
        List<Voucher> visible = new ArrayList<>();
        for (Voucher voucher : allVouchers) {
            boolean reserved = reservations.isReserved(voucher.code, now) && voucher.guestCount == 0;
            boolean include = switch (filter) {
                case AVAILABLE -> voucher.isAvailable(now) && !reserved;
                case ACTIVE -> voucher.state(now) == Voucher.State.ACTIVE || reserved;
                case UNAVAILABLE -> voucher.state(now) == Voucher.State.UNAVAILABLE;
            };
            if (include) visible.add(voucher);
        }
        adapter.replace(visible);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(getColor(R.color.navy));
        button.setBackgroundResource(R.drawable.brand_button);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        return button;
    }

    private ImageButton iconButton(int drawable, int description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawable);
        button.setContentDescription(getString(description));
        button.setBackgroundResource(R.drawable.brand_button);
        button.setPadding(dp(9), dp(9), dp(9), dp(9));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return button;
    }

    private LinearLayout.LayoutParams iconParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        params.leftMargin = dp(4);
        return params;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
