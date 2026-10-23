package com.nordgas.ngsvisitor;

import android.os.SystemClock;

public final class AdminSession {
    private static final long SESSION_MILLIS = 5 * 60_000L;
    private static volatile long unlockedUntil;

    private AdminSession() {}

    public static void unlock() {
        unlockedUntil = SystemClock.elapsedRealtime() + SESSION_MILLIS;
    }

    public static boolean isUnlocked() {
        return SystemClock.elapsedRealtime() < unlockedUntil;
    }

    public static void clear() {
        unlockedUntil = 0L;
    }
}
