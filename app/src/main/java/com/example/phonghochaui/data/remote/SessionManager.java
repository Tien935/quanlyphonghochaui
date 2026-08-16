package com.example.phonghochaui.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFERENCES_NAME =
            "haui_classroom_session";

    private static final String KEY_ACCESS_TOKEN =
            "access_token";

    private static final String KEY_REFRESH_TOKEN =
            "refresh_token";

    private static final String KEY_USER_ID =
            "user_id";

    private static final String KEY_USER_EMAIL =
            "user_email";

    private static final String KEY_USER_ROLE =
            "user_role";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context
                .getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );
    }

    public void saveAccessToken(String accessToken) {
        preferences.edit()
                .putString(
                        KEY_ACCESS_TOKEN,
                        accessToken == null ? "" : accessToken
                )
                .apply();
    }

    public void updateTokens(
            String accessToken,
            String refreshToken
    ) {
        SharedPreferences.Editor editor =
                preferences.edit()
                        .putString(
                                KEY_ACCESS_TOKEN,
                                safeValue(accessToken)
                        );

        if (refreshToken != null
                && !refreshToken.trim().isEmpty()) {
            editor.putString(
                    KEY_REFRESH_TOKEN,
                    refreshToken.trim()
            );
        }

        editor.apply();
    }

    public void saveSession(
            String accessToken,
            String refreshToken,
            String userId,
            String email
    ) {
        preferences.edit()
                .putString(
                        KEY_ACCESS_TOKEN,
                        safeValue(accessToken)
                )
                .putString(
                        KEY_REFRESH_TOKEN,
                        safeValue(refreshToken)
                )
                .putString(
                        KEY_USER_ID,
                        safeValue(userId)
                )
                .putString(
                        KEY_USER_EMAIL,
                        safeValue(email)
                )
                .apply();
    }

    public void saveUserRole(String role) {
        preferences.edit()
                .putString(
                        KEY_USER_ROLE,
                        safeValue(role)
                )
                .apply();
    }

    public String getAccessToken() {
        return preferences.getString(
                KEY_ACCESS_TOKEN,
                ""
        );
    }

    public String getRefreshToken() {
        return preferences.getString(
                KEY_REFRESH_TOKEN,
                ""
        );
    }

    public String getUserRole() {
        return preferences.getString(
                KEY_USER_ROLE,
                ""
        );
    }

    public String getUserId() {
        return preferences.getString(
                KEY_USER_ID,
                ""
        );
    }

    public String getUserEmail() {
        return preferences.getString(
                KEY_USER_EMAIL,
                ""
        );
    }

    public boolean hasSession() {
        return !getAccessToken().isEmpty();
    }

    public void clearSession() {
        preferences.edit()
                .clear()
                .apply();
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}