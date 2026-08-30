package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.AuthResponse;
import com.example.phonghochaui.data.model.LoginRequest;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.example.phonghochaui.data.remote.SupabaseConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator loginProgress;
    private TextView tvLoginError;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.loginRoot),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        bindViews();

        sessionManager = new SessionManager(this);

        String savedRole = sessionManager.getUserRole();

        if (sessionManager.hasSession()
                && isSupportedRole(savedRole)) {
            openHome(savedRole, false);
            return;
        }

        if (SupabaseConfig.isConfigured()) {
            apiService = RetrofitClient.getApiService(this);
        } else {
            showError(
                    getString(
                            R.string.error_configuration_missing
                    )
            );

            btnLogin.setEnabled(false);
        }

        btnLogin.setOnClickListener(
                view -> attemptLogin()
        );

        etPassword.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId
                            == EditorInfo.IME_ACTION_DONE) {
                        attemptLogin();
                        return true;
                    }

                    return false;
                }
        );
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgress = findViewById(R.id.loginProgress);
        tvLoginError = findViewById(R.id.tvLoginError);
    }

    private void attemptLogin() {
        clearErrors();

        if (apiService == null) {
            showError(
                    getString(
                            R.string.error_configuration_missing
                    )
            );
            return;
        }

        String email = textOf(etEmail);
        String password = textOf(etPassword);

        if (!validateInput(email, password)) {
            return;
        }

        setLoading(true);

        apiService.signInWithPassword(
                "password",
                new LoginRequest(email, password)
        ).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                AuthResponse authResponse = response.body();

                if (!response.isSuccessful()
                        || authResponse == null
                        || authResponse.getAccessToken() == null
                        || authResponse.getUser() == null) {

                    setLoading(false);

                    showError(
                            readApiError(
                                    response.errorBody(),
                                    R.string.error_login_failed
                            )
                    );

                    return;
                }

                sessionManager.saveSession(
                        authResponse.getAccessToken(),
                        authResponse.getRefreshToken(),
                        authResponse.getUser().getId(),
                        authResponse.getUser().getEmail()
                );

                loadUserRole(
                        authResponse.getUser().getId()
                );
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showError(networkError(throwable));
            }
        });
    }

    private void loadUserRole(String userId) {
        apiService.getProfileRole(
                "eq." + userId,
                "role",
                1
        ).enqueue(new Callback<JsonArray>() {

            @Override
            public void onResponse(
                    Call<JsonArray> call,
                    Response<JsonArray> response
            ) {
                setLoading(false);

                JsonArray profiles = response.body();

                if (!response.isSuccessful()) {
                    sessionManager.clearSession();

                    showError(
                            readApiError(
                                    response.errorBody(),
                                    R.string.error_profile_missing
                            )
                    );

                    return;
                }

                if (profiles == null
                        || profiles.size() == 0) {
                    sessionManager.clearSession();

                    showError(
                            getString(
                                    R.string.error_profile_missing
                            )
                    );

                    return;
                }

                JsonObject profile =
                        profiles.get(0).getAsJsonObject();

                JsonElement roleElement =
                        profile.get("role");

                String role = roleElement == null
                        ? ""
                        : roleElement
                        .getAsString()
                        .trim();

                if (!isSupportedRole(role)) {
                    sessionManager.clearSession();

                    showError(
                            getString(
                                    R.string.error_role_invalid
                            )
                    );

                    return;
                }

                sessionManager.saveUserRole(role);
                openHome(role, true);
            }

            @Override
            public void onFailure(
                    Call<JsonArray> call,
                    Throwable throwable
            ) {
                setLoading(false);
                sessionManager.clearSession();
                showError(networkError(throwable));
            }
        });
    }

    private boolean validateInput(
            String email,
            String password
    ) {
        boolean valid = true;

        if (email.isEmpty()) {
            tilEmail.setError(
                    getString(
                            R.string.error_email_required
                    )
            );

            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            tilEmail.setError(
                    getString(
                            R.string.error_email_invalid
                    )
            );

            valid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(
                    getString(
                            R.string.error_password_required
                    )
            );

            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(
                    getString(
                            R.string.error_password_short
                    )
            );

            valid = false;
        }

        return valid;
    }

    private boolean isSupportedRole(String role) {
        return "admin".equals(role)
                || "student".equals(role);
    }

    private void openHome(
            String role,
            boolean showSuccessMessage
    ) {
        if (showSuccessMessage) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.login_success_with_role,
                            role
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }

        Class<?> destination =
                "admin".equals(role)
                        ? AdminHomeActivity.class
                        : StudentHomeActivity.class;

        Intent intent =
                new Intent(this, destination);

        intent.putExtra("USER_ROLE", role);

        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        tvLoginError.setText("");
        tvLoginError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvLoginError.setText(message);
        tvLoginError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        btnLogin.setEnabled(!loading);

        btnLogin.setText(
                loading
                        ? R.string.login_loading
                        : R.string.login_button
        );

        loginProgress.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private String readApiError(
            ResponseBody errorBody,
            int fallbackString
    ) {
        if (errorBody == null) {
            return getString(fallbackString);
        }

        try {
            JsonElement element =
                    JsonParser.parseString(
                            errorBody.string()
                    );

            if (!element.isJsonObject()) {
                return getString(fallbackString);
            }

            JsonObject error =
                    element.getAsJsonObject();

            String[] fields = {
                    "error_description",
                    "msg",
                    "message",
                    "error"
            };

            for (String field : fields) {
                JsonElement value = error.get(field);

                if (value != null
                        && !value.isJsonNull()) {

                    String message =
                            value.getAsString();

                    if (!message.trim().isEmpty()) {
                        if ("User is banned".equalsIgnoreCase(message.trim())) {
                            return "Tài khoản đã bị cấm";
                        }
                        return message;
                    }
                }
            }
        } catch (IOException
                 | RuntimeException ignored) {
            // Sử dụng thông báo mặc định bên dưới.
        }

        return getString(fallbackString);
    }

    private String networkError(
            Throwable throwable
    ) {
        String detail = throwable.getMessage();

        if (detail == null
                || detail.trim().isEmpty()) {
            return getString(
                    R.string.error_login_failed
            );
        }

        return getString(
                R.string.error_network,
                detail
        );
    }

    private String textOf(
            TextInputEditText editText
    ) {
        return editText.getText() == null
                ? ""
                : editText
                .getText()
                .toString()
                .trim();
    }
}