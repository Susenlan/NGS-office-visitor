package com.nordgas.ngsvisitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class AdminCredentialStore {
    private static final String PREFS = "ngs_admin_credentials";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 60_000L;
    private final SharedPreferences prefs;

    public AdminCredentialStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasCredentials() {
        return !prefs.getString("username", "").isEmpty()
                && !prefs.getString("salt", "").isEmpty()
                && !prefs.getString("hash", "").isEmpty();
    }

    public void createOrReplace(String username, char[] password) {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.derive(password, salt);
        prefs.edit()
                .putString("username", username.trim())
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt("failures", 0)
                .putLong("locked_until", 0L)
                .apply();
    }

    public boolean verify(String username, char[] password) {
        if (remainingLockMillis() > 0 || !hasCredentials()) return false;
        try {
            String storedUsername = prefs.getString("username", "");
            boolean usernameMatches = MessageDigest.isEqual(
                    storedUsername.getBytes(StandardCharsets.UTF_8),
                    username.trim().getBytes(StandardCharsets.UTF_8));
            byte[] salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP);
            byte[] hash = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP);
            boolean passwordMatches = PasswordHasher.verify(password, salt, hash);
            return usernameMatches && passwordMatches;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void recordFailure() {
        int failures = prefs.getInt("failures", 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();
        if (failures >= MAX_ATTEMPTS) {
            editor.putInt("failures", 0)
                    .putLong("locked_until", System.currentTimeMillis() + LOCK_MILLIS);
        } else {
            editor.putInt("failures", failures);
        }
        editor.apply();
    }

    public void clearFailures() {
        prefs.edit().putInt("failures", 0).putLong("locked_until", 0L).apply();
    }

    public long remainingLockMillis() {
        return Math.max(0L, prefs.getLong("locked_until", 0L) - System.currentTimeMillis());
    }
}
