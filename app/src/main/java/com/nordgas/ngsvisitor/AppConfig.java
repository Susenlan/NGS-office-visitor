package com.nordgas.ngsvisitor;

public final class AppConfig {
    public final String baseUrl;
    public final String siteId;
    public final String apiKey;
    public final String certificateSha256;
    public final int fallbackGuestLimit;

    public AppConfig(String baseUrl, String siteId, String apiKey,
                     String certificateSha256, int fallbackGuestLimit) {
        this.baseUrl = baseUrl;
        this.siteId = siteId;
        this.apiKey = apiKey;
        this.certificateSha256 = certificateSha256;
        this.fallbackGuestLimit = fallbackGuestLimit;
    }

    public boolean isComplete() {
        return baseUrl != null && !baseUrl.trim().isEmpty()
                && apiKey != null && !apiKey.trim().isEmpty();
    }
}
