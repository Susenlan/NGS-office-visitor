package com.nordgas.ngsvisitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureConfigStore {
    private static final String PREFS = "ngs_visitor_config";
    private static final String KEY_ALIAS = "ngs_visitor_api_key";
    private final SharedPreferences prefs;

    public SecureConfigStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public AppConfig load() {
        return new AppConfig(
                prefs.getString("base_url", ""),
                prefs.getString("site_id", ""),
                decrypt(prefs.getString("api_key", "")),
                prefs.getString("cert_sha256", ""),
                prefs.getInt("fallback_limit", 5));
    }

    public void save(AppConfig config, boolean keepExistingKeyWhenBlank) {
        String encryptedKey = prefs.getString("api_key", "");
        if (!config.apiKey.trim().isEmpty() || !keepExistingKeyWhenBlank) {
            encryptedKey = encrypt(config.apiKey);
        }
        prefs.edit()
                .putString("base_url", config.baseUrl.trim())
                .putString("site_id", config.siteId.trim())
                .putString("api_key", encryptedKey)
                .putString("cert_sha256", normalizeFingerprint(config.certificateSha256))
                .putInt("fallback_limit", Math.max(1, config.fallbackGuestLimit))
                .apply();
    }

    public void saveSiteId(String siteId) {
        prefs.edit().putString("site_id", siteId).apply();
    }

    public boolean hasApiKey() {
        return !prefs.getString("api_key", "").trim().isEmpty();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return (SecretKey) store.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    private String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "." +
                    Base64.encodeToString(ciphertext, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("无法安全保存 API Key", e);
        }
    }

    private String decrypt(String stored) {
        if (stored == null || stored.trim().isEmpty()) return "";
        try {
            String[] parts = stored.split("\\.", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String normalizeFingerprint(String value) {
        return value == null ? "" : value.replace(":", "").replace(" ", "").toUpperCase();
    }
}
