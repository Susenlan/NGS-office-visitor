package com.nordgas.ngsvisitor;

public final class ApiUrl {
    private ApiUrl() {}

    public static String normalizeRoot(String input) {
        if (input == null) return "";
        String value = input.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.endsWith("/proxy/network/integration")) return value;
        return value + "/proxy/network/integration";
    }
}

