package com.nordgas.ngsvisitor;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

public final class ReservationStore {
    private static final String PREFS = "ngs_voucher_reservations";
    private static final Duration HOLD_TIME = Duration.ofMinutes(30);
    private final SharedPreferences prefs;

    public ReservationStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isReserved(String code, Instant now) {
        long timestamp = prefs.getLong(key(code), 0L);
        if (timestamp == 0L) return false;
        Instant reservedAt = Instant.ofEpochMilli(timestamp);
        if (reservedAt.plus(HOLD_TIME).isAfter(now)) return true;
        clear(code);
        return false;
    }

    public void reserve(String code) {
        prefs.edit().putLong(key(code), System.currentTimeMillis()).apply();
    }

    public void clear(String code) {
        prefs.edit().remove(key(code)).apply();
    }

    private static String key(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("v_");
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
