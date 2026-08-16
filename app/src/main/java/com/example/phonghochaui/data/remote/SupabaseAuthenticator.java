package com.example.phonghochaui.data.remote;

import com.example.phonghochaui.data.model.AuthResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class SupabaseAuthenticator implements Authenticator {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private final SessionManager sessionManager;
    private final OkHttpClient refreshClient;
    private final Gson gson;
    private final Object refreshLock = new Object();

    public SupabaseAuthenticator(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.refreshClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (responseCount(response) >= 2) {
            return null;
        }

        String failedToken = bearerToken(response.request());

        synchronized (refreshLock) {
            String currentToken = clean(sessionManager.getAccessToken());

            // Một yêu cầu khác đã làm mới token trong lúc yêu cầu này chờ.
            if (!currentToken.isEmpty() && !currentToken.equals(failedToken)) {
                return retryRequest(response.request(), currentToken);
            }

            String refreshToken = clean(sessionManager.getRefreshToken());

            if (refreshToken.isEmpty()) {
                return null;
            }

            String newAccessToken = refreshAccessToken(refreshToken);

            if (newAccessToken.isEmpty()) {
                return null;
            }

            return retryRequest(response.request(), newAccessToken);
        }
    }

    private String refreshAccessToken(String refreshToken) {
        JsonObject json = new JsonObject();
        json.addProperty("refresh_token", refreshToken);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(json),
                JSON_MEDIA_TYPE
        );

        Request refreshRequest = new Request.Builder()
                .url(
                        SupabaseConfig.getBaseUrl()
                                + "auth/v1/token?grant_type=refresh_token"
                )
                .header("apikey", SupabaseConfig.getPublishableKey())
                .header("Accept", "application/json")
                .post(requestBody)
                .build();

        try (Response refreshResponse = refreshClient
                .newCall(refreshRequest)
                .execute()) {

            if (!refreshResponse.isSuccessful()
                    || refreshResponse.body() == null) {

                if (refreshResponse.code() == 400
                        || refreshResponse.code() == 401
                        || refreshResponse.code() == 403) {
                    sessionManager.clearSession();
                }

                return "";
            }

            AuthResponse authResponse = gson.fromJson(
                    refreshResponse.body().charStream(),
                    AuthResponse.class
            );

            if (authResponse == null
                    || clean(authResponse.getAccessToken()).isEmpty()) {
                return "";
            }

            sessionManager.updateTokens(
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken()
            );

            return clean(authResponse.getAccessToken());

        } catch (IOException | RuntimeException ignored) {
            // Không xóa phiên nếu chỉ gặp lỗi mạng tạm thời.
            return "";
        }
    }

    private Request retryRequest(
            Request originalRequest,
            String accessToken
    ) {
        return originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
    }

    private String bearerToken(Request request) {
        String authorization = request.header("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return "";
        }

        return clean(
                authorization.substring("Bearer ".length())
        );
    }

    private int responseCount(Response response) {
        int count = 1;
        Response previous = response.priorResponse();

        while (previous != null) {
            count++;
            previous = previous.priorResponse();
        }

        return count;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}