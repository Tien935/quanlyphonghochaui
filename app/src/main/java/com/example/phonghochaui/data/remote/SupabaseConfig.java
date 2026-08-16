package com.example.phonghochaui.data.remote;

import com.example.phonghochaui.BuildConfig;

public final class SupabaseConfig {

    private static final String URL_PLACEHOLDER = "your-project-ref";
    private static final String KEY_PLACEHOLDER = "YOUR_PUBLISHABLE_KEY";

    private SupabaseConfig() {
        // Utility class.
    }

    public static String getBaseUrl() {
        String url = BuildConfig.SUPABASE_URL.trim();
        return url.endsWith("/") ? url : url + "/";
    }

    public static String getPublishableKey() {
        return BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim();
    }

    public static boolean isConfigured() {
        String url = getBaseUrl();
        String key = getPublishableKey();

        return url.startsWith("https://")
                && url.contains(".supabase.co/")
                && !url.contains(URL_PLACEHOLDER)
                && !key.isEmpty()
                && !KEY_PLACEHOLDER.equals(key);
    }

    public static boolean usesLegacyAnonKey() {
        String key = getPublishableKey();
        return key.startsWith("eyJ") && key.split("\\.").length == 3;
    }
}
