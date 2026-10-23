package com.nordgas.ngsvisitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class UniFiApiClient {
    private final AppConfig config;

    public UniFiApiClient(AppConfig config) {
        this.config = config;
    }

    public String discoverSiteId() throws Exception {
        Object root = getJson(ApiUrl.normalizeRoot(config.baseUrl) + "/v1/sites?limit=200");
        JSONArray data = extractDataArray(root);
        if (data.length() == 0) throw new IOException("NO_SITES");
        return data.getJSONObject(0).getString("id");
    }

    public List<Voucher> loadVouchers(String siteId) throws Exception {
        String url = ApiUrl.normalizeRoot(config.baseUrl) + "/v1/sites/" + siteId +
                "/hotspot/vouchers?limit=1000";
        JSONArray data = extractDataArray(getJson(url));
        List<Voucher> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            int limit = optionalInt(item, "authorizedGuestLimit", config.fallbackGuestLimit);
            result.add(new Voucher(
                    item.optString("id", ""),
                    formatCode(item.optString("code", "")),
                    item.optString("name", ""),
                    limit,
                    optionalInt(item, "authorizedGuestCount", 0),
                    parseInstant(item.optString("activatedAt", "")),
                    parseInstant(item.optString("expiresAt", "")),
                    item.optBoolean("expired", false)));
        }
        return result;
    }

    private Object getJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("HTTPS_REQUIRED");
        }
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        configurePinnedCertificate(connection);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(20_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-API-Key", config.apiKey);
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String body = readAll(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP_STATUS:" + code);
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        return new JSONObject(trimmed);
    }

    private void configurePinnedCertificate(HttpsURLConnection connection) throws Exception {
        String expected = SecureConfigStore.normalizeFingerprint(config.certificateSha256);
        if (expected.trim().isEmpty()) return;

        X509TrustManager pinningManager = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                if (chain != null && chain.length > 0) chain[0].checkValidity();
                if (chain == null || chain.length == 0 || !expected.equals(fingerprint(chain[0]))) {
                    throw new CertificateException("CERTIFICATE_MISMATCH");
                }
            }

            @Override public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{pinningManager}, null);
        connection.setSSLSocketFactory(context.getSocketFactory());
        HostnameVerifier verifier = (hostname, session) -> {
            try {
                return expected.equals(fingerprint((X509Certificate) session.getPeerCertificates()[0]));
            } catch (SSLPeerUnverifiedException | CertificateException e) {
                return false;
            }
        };
        connection.setHostnameVerifier(verifier);
    }

    private static String fingerprint(X509Certificate certificate) throws CertificateException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02X", b));
            return out.toString();
        } catch (Exception e) {
            throw new CertificateException(e);
        }
    }

    static JSONArray extractDataArray(Object rootValue) {
        if (rootValue instanceof JSONArray) return (JSONArray) rootValue;
        if (!(rootValue instanceof JSONObject)) return new JSONArray();
        JSONObject root = (JSONObject) rootValue;
        Object data = root.opt("data");
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            Object nested = ((JSONObject) data).opt("data");
            if (nested instanceof JSONArray) return (JSONArray) nested;
        }
        Object vouchers = root.opt("vouchers");
        if (vouchers instanceof JSONArray) return (JSONArray) vouchers;
        return new JSONArray();
    }

    private static int optionalInt(JSONObject item, String key, int fallback) {
        if (!item.has(key) || item.isNull(key)) return fallback;
        return item.optInt(key, fallback);
    }

    private static Instant parseInstant(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    static String formatCode(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (digits.length() == 10) return digits.substring(0, 5) + "-" + digits.substring(5);
        return raw == null ? "" : raw;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }
}
