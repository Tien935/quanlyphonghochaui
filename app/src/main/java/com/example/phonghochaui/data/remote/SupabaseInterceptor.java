package com.example.phonghochaui.data.remote;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SupabaseInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public SupabaseInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder()
                .header("apikey", SupabaseConfig.getPublishableKey())
                .header("Accept", "application/json");

        String accessToken = sessionManager.getAccessToken();

        if (accessToken != null && !accessToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + accessToken.trim());
        } else if (SupabaseConfig.usesLegacyAnonKey()) {
            // Legacy anon keys are JWTs and may be used as the anonymous bearer token.
            requestBuilder.header(
                    "Authorization",
                    "Bearer " + SupabaseConfig.getPublishableKey()
            );
        }

        return chain.proceed(requestBuilder.build());
    }
}
