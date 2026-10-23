package com.nordgas.ngsvisitor;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SystemClock;

public final class ClipboardSecurity {
    private static final long CLEAR_DELAY_MILLIS = 60_000L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static volatile String lastVoucher;
    private static volatile long voucherExpiresAt;

    private ClipboardSecurity() {}

    public static void copyVoucher(Context context, String code) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("NGS Visitor voucher", code);
        PersistableBundle extras = new PersistableBundle();
        String sensitiveKey = Build.VERSION.SDK_INT >= 33
                ? ClipDescription.EXTRA_IS_SENSITIVE : "android.content.extra.IS_SENSITIVE";
        extras.putBoolean(sensitiveKey, true);
        clip.getDescription().setExtras(extras);
        clipboard.setPrimaryClip(clip);
        lastVoucher = code;
        voucherExpiresAt = SystemClock.elapsedRealtime() + CLEAR_DELAY_MILLIS;
        if (new SecurityPreferences(context).privacyEnabled()) {
            Context appContext = context.getApplicationContext();
            HANDLER.postDelayed(() -> clearIfVoucher(appContext), CLEAR_DELAY_MILLIS);
        }
    }

    public static void clearExpiredVoucher(Context context) {
        if (lastVoucher != null && SystemClock.elapsedRealtime() >= voucherExpiresAt) {
            clearIfVoucher(context);
        }
    }

    public static void clearIfVoucher(Context context) {
        String expected = lastVoucher;
        if (expected == null || !new SecurityPreferences(context).privacyEnabled()) return;
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            CharSequence current = clip.getItemAt(0).coerceToText(context);
            if (current == null || !expected.contentEquals(current)) return;
            if (Build.VERSION.SDK_INT >= 28) {
                clipboard.clearPrimaryClip();
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
            }
            lastVoucher = null;
            voucherExpiresAt = 0L;
        } catch (SecurityException ignored) {
            // The OS may deny clipboard access when the app is no longer foreground.
        }
    }
}
